# SPARQL Rewrite Framework and 50 Rules

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document according to
`.agent/PLANS.md`.

## Purpose / Big Picture

RDF4J LMDB already has many query optimizer rewrites, but they are spread across pipeline optimizers, LMDB helper
classes, and Cascades rules. This work adds one stable algebra-level safety framework that can represent all 50 SPARQL
rewrite families before implementing the missing rules one by one. After the framework exists, each rewrite candidate
must explain why it preserves visible bindings, multiplicity, ordering, errors, graph scope, and any metadata
assumptions before the optimizer may use it.

The observable result is internal but testable: focused optimizer tests should show unsafe rewrites being rejected,
safe rewrites carrying explicit proof certificates, and the LMDB optimizer pipeline continuing to produce parent-safe
algebra plans.

## Progress

- [x] (2026-06-14 08:08 Europe/Oslo) Ran required root quick install before edits.
- [x] (2026-06-14 08:11 Europe/Oslo) Created this ExecPlan and recorded the current coverage review.
- [x] (2026-06-14 08:18 Europe/Oslo) Added red/green framework tests for certificate dimensions, rule gating, and
      `RuleProof` metric serialization.
- [x] (2026-06-14 08:21 Europe/Oslo) Implemented shared framework types for modes, assumptions, certificates,
      metadata defaults, and the 50-entry rule catalog.
- [x] (2026-06-14 08:26 Europe/Oslo) Extended Cascades `RuleProof` and LMDB `LmdbRewriteProof` to carry optional
      generic rewrite certificates while preserving old constructors and metric fragments.
- [x] (2026-06-14 08:50 Europe/Oslo) Fixed the pre-existing LMDB/sketch planner confidence-floor regression that was
      blocking `LmdbOptimizerPipelineTest`.
- [x] (2026-06-14 08:58 Europe/Oslo) Migrated the existing independent-OPTIONAL reordering rewrite to emit a generic
      rule-27 certificate and marked rule 27 as partially implemented in the catalog.
- [x] (2026-06-14 09:08 Europe/Oslo) Migrated duplicate-UNION idempotence under set semantics to emit a generic
      rule-19 certificate that explicitly records multiplicity is not preserved and requires duplicate-insensitive
      context.
- [x] (2026-06-14 10:39 Europe/Oslo) Migrated top-level ASK OPTIONAL elimination to emit a generic rule-28
      certificate that records existence-only safety and non-preserved multiplicity.
- [x] (2026-06-14 11:01 Europe/Oslo) Migrated duplicate LEFT JOIN idempotence under set semantics to emit a generic
      rule-26 certificate for dead-OPTIONAL-style elimination in duplicate-insensitive context.
- [x] (2026-06-14 11:09 Europe/Oslo) Migrated duplicate JOIN idempotence under set semantics to emit a generic rule-5
      certificate, conservatively recording that multiplicity preservation depends on duplicate-insensitive context.
- [x] (2026-06-14 11:27 Europe/Oslo) Migrated null-rejecting OPTIONAL-to-JOIN rewriting to emit a generic rule-24
      certificate while preserving its existing optional-binding telemetry.
- [x] (2026-06-14 11:50 Europe/Oslo) Migrated OPTIONAL `!BOUND` anti-join rewriting to emit a generic rule-25
      certificate on the selected anti-join alternative.
- [x] (2026-06-14 12:29 Europe/Oslo) Fixed the isolated scoped-union distribution regression exposed after the
      rule-25 migration by preserving rule-24 mandatory-right locality across scoped fanout.
- [x] (2026-06-14 12:47 Europe/Oslo) Migrated set-semantics finite membership semi-filter rewriting to emit a generic
      rule-23 certificate for the existing COUNT DISTINCT-safe early membership join.
- [x] (2026-06-14 12:52 Europe/Oslo) Migrated filter-simplifier finite membership semi-filter rewriting to emit the
      same generic rule-23 certificate for its COUNT DISTINCT-safe early membership join.
- [x] (2026-06-14 13:16 Europe/Oslo) Migrated existing `FILTER IN` finite-relation materialization to emit a generic
      rule-10 certificate on the generated `BindingSetAssignment`.
- [x] (2026-06-14 13:21 Europe/Oslo) Migrated existing equality-disjunction finite-relation materialization to emit a
      generic rule-11 certificate on the generated `BindingSetAssignment`.
- [x] (2026-06-14 13:27 Europe/Oslo) Implemented tuple-disjunction finite-relation materialization with a generic
      rule-12 certificate on the generated `BindingSetAssignment`.
- [x] (2026-06-14 13:34 Europe/Oslo) Implemented duplicate-insensitive `VALUES` row deduplication with a generic
      rule-13 certificate on the deduplicated `BindingSetAssignment`.
- [x] (2026-06-14 13:44 Europe/Oslo) Migrated independent `VALUES` correlation materialization to emit a generic
      rule-14 certificate for term-identity finite relations.
- [x] (2026-06-14 14:07 Europe/Oslo) Migrated finite variable-predicate domain anchoring to emit a generic rule-15
      certificate when finite IRI `VALUES` bind the predicate variable.
- [x] (2026-06-14 14:20 Europe/Oslo) Implemented the binary same-shape UNION constants-to-VALUES alternative with a
      generic rule-16 certificate and marked rule 16 partial in the catalog.
- [x] (2026-06-14 14:31 Europe/Oslo) Implemented exact binary UNION common-prefix factoring for pure join regions with
      a generic rule-17 certificate in the standard Cascades rule registry.
- [x] (2026-06-14 14:37 Europe/Oslo) Implemented exact binary UNION common-suffix factoring for pure join regions with
      a generic rule-18 certificate in the standard Cascades rule registry.
- [x] (2026-06-14 14:43 Europe/Oslo) Implemented a guarded subsumed-UNION-branch elimination slice for assured
      `BOUND` filters under duplicate-insensitive Cascades goals with a generic rule-20 certificate.
- [x] (2026-06-14 14:50 Europe/Oslo) Migrated existing projection-over-UNION distribution to emit a generic rule-21
      certificate for branch-local projections.
- [x] (2026-06-14 14:58 Europe/Oslo) Implemented guarded branch-local DISTINCT alternatives for non-scope-changing
      UNION branches under duplicate-insensitive Cascades goals with a generic rule-22 certificate.
- [x] (2026-06-14 15:04 Europe/Oslo) Implemented the portable key-only `NOT EXISTS` to `MINUS` slice with distinct
      projected anti-join keys and a generic rule-30 certificate.
- [x] (2026-06-14 15:10 Europe/Oslo) Implemented a narrow rule-31 slice that pushes local `sameTerm` constants into
      `NOT EXISTS` statement patterns while rejecting outer-correlated variables.
- [x] (2026-06-14 15:13 Europe/Oslo) Reviewed RDF4J property-path algebra and marked rules 32-34 as delegated to
      existing SPARQL parser lowering for sequence, inverse, and alternation paths.
- [x] (2026-06-14 15:20 Europe/Oslo) Implemented guarded rule-35 positive-closure decomposition under
      duplicate-insensitive Cascades goals and marked rule 35 partial in the catalog.
- [x] (2026-06-14 15:55 Europe/Oslo) Implemented rule-40 aggregate-free `GROUP BY` to `DISTINCT(Project(...))`
      lowering with a generic certificate and marked rule 40 partial in the catalog.
- [x] (2026-06-14 16:14 Europe/Oslo) Implemented the narrow RDF4J algebra rule-41 fixed graph substitution for
      `sameTerm` filters over statement-pattern context variables.
- [x] (2026-06-14 16:54 Europe/Oslo) Reviewed rule 42 and marked repeated graph context hoisting as delegated to
      existing SPARQL parser lowering into statement-pattern contexts.
- [x] (2026-06-14 17:42 Europe/Oslo) Implemented rule-44 static local `VALUES` pushdown into fixed compatible
      `SERVICE` endpoints while preserving the original local `VALUES` outside the service.
- [x] (2026-06-14 17:57 Europe/Oslo) Implemented a narrow rule-45 `BOUND` filter pushdown into fixed compatible
      `SERVICE` endpoints with endpoint expression-compatibility metadata.
- [x] (2026-06-14 18:14 Europe/Oslo) Implemented a narrow rule-46 remote projection pushdown for identity projections
      over fixed compatible `SERVICE` endpoints.
- [x] (2026-06-14 18:44 Europe/Oslo) Implemented a narrow rule-47 variable `SERVICE` expansion over complete endpoint
      metadata with endpoint binding preservation.
- [x] (2026-06-14 19:10 Europe/Oslo) Implemented a narrow rule-48 modifier-free subquery inlining under a preserving
      outer projection.
- [x] (2026-06-14 19:35 Europe/Oslo) Implemented a narrow rule-49 drop-unused-subquery-vars rewrite under a preserving
      outer projection.
- [x] (2026-06-14 19:50 Europe/Oslo) Implemented a narrow rule-50 unobservable inner-subquery `ORDER BY` removal
      under a preserving outer projection.
- [ ] Migrate individual existing rewrite call sites to populate generic certificates.
- [ ] Implement portable term, filter, VALUES, BGP, UNION, OPTIONAL, path, graph, service, subquery, and modifier rules
      incrementally.
- [ ] Add a rule matrix test proving every rewrite ID is implemented, metadata-gated, or intentionally unsupported.

## Surprises & Discoveries

- Observation: The current worktree already has uncommitted LMDB optimizer changes in
  `LmdbQueryOptimizerPipeline.java`, `LmdbRewriteProof.java`, and `LmdbOptimizerPipelineTest.java`, plus untracked
  `LmdbEligibilitySemiJoinOptimizer.java` and `LmdbTupleExprFacts.java`.
  Evidence: `git status --short --untracked-files=all` on 2026-06-14 showed those files before this ExecPlan was
  created.
- Observation: Existing proof telemetry is narrower than the requested framework. `LmdbRewriteProof` records kind,
  equivalence scope, facts, and reason; Cascades `RuleProof` records rule id, kind, semantic scope, facts, and reason.
  Neither records the requested booleans for visible vars, multiplicity, order, errors, and graph scope.
  Evidence: inspection of `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRewriteProof.java` and
  `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/RuleProof.java`.
- Observation: The branch is `GH-0000-lmdb-predicate-guarantees`, not `main` or `master`, so implementation can proceed
  in the current workspace without a branch switch.
  Evidence: `git branch --show-current`.
- Observation: `LmdbRewriteProofTest` already existed and covered old proof metric behavior. The certificate test must
  remain additive; do not replace the original tests.
  Evidence: the restored class now passes with 4 tests, including the three pre-existing proof tests.
- Observation: Full `LmdbOptimizerPipelineTest` currently fails in
  `lmdbSketchEnrichmentKeepsNonExactFiniteAnchorZeroCostFloor` because `plannedCostConfidence` is `0.9` where the test
  expects low confidence. This failure is in pre-existing modified LMDB optimizer coverage and does not involve the
  new certificate framework.
  Evidence: `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOptimizerPipelineTest --retain-logs` on 2026-06-14.
- Observation: The confidence-floor failure was not in LMDB proof telemetry. `SketchJoinOrderPlanner` applied the
  one-row cost floor from raw joined-prefix rows, but computed cost-vector confidence from an already-floored value in
  the path where the current factor had positive rows. Passing raw rows into the vector builder lets the existing
  non-exact-zero confidence cap apply.
  Evidence: `LmdbOptimizerPipelineTest#lmdbSketchEnrichmentKeepsNonExactFiniteAnchorZeroCostFloor` and
  `SketchBasedJoinEstimatorJoinOrderPlannerTest#finiteAnchorSketchZeroKeepsPlanningFloorWithoutClaimingExactRows`
  both pass after the fix.
- Observation: The isolated `rewrittenOptionalNameAnchorDistributesConnectedGuardIntoScopedUnionBranches` failure was
  caused by rule-24 support running before the small-literal anchor pass. It converted the null-rejecting
  `FILTER(OPTIONAL)` to `Filter(Join(left,right))`, leaving a scoped union between the finite seed and the now-mandatory
  right-side lookup. The sketch optimizer then correctly duplicated only the finite seed into union branches and left
  the lookup as a post-union suffix.
  Evidence: a temporary diagnostic run printed post-simplifier algebra shaped as
  `Join(optNameValues, Join(Join(userValues, scopedUnion), nameLookup))`; after the fix
  `LmdbSketchJoinOptimizerTest` passes with 41 tests.

## Decision Log

- Decision: Implement the safety framework before adding new rewrite behavior.
  Rationale: The user's explicit priority is to represent all 50 rewrite rules without changing the framework later.
  Date/Author: 2026-06-14 / Codex.
- Decision: Work in the existing checkout instead of creating a new worktree.
  Rationale: The task must preserve and review current uncommitted LMDB optimizer work. A clean worktree would omit
  those changes and risk duplicating or overwriting them.
  Date/Author: 2026-06-14 / Codex.
- Decision: Keep rewrite execution on RDF4J `TupleExpr`/Cascades algebra, not SPARQL string rewrites.
  Rationale: SPARQL multiplicity, hidden variables, graph scope, and solution modifiers are represented in algebra and
  cannot be handled safely by textual substitutions.
  Date/Author: 2026-06-14 / Codex.

## Outcomes & Retrospective

The framework milestone is complete and the existing rewrite call sites are being migrated incrementally. The initial
root install passed and the all-50 rollout is captured in this self-contained ExecPlan.

Framework milestone outcome: the generic certificate surface exists, mode/assumption guards are tested, the empty
metadata default disables schema and federation facts, the 50-rule catalog is executable test coverage, and both
Cascades and LMDB proof records can carry optional generic certificate safety dimensions. Existing individual rewrite
call sites still need incremental migration to populate certificates.

Confidence-floor regression outcome: the LMDB pipeline class is green again. The planner now preserves low confidence
when a non-exact zero cardinality is costed with a one-row planning floor, including the joined-prefix case where the
current factor itself has positive rows.

Rule-27 migration outcome: `LmdbOptionalNormalFormOptimizer` now emits a generic certificate alongside its legacy
`OPTIONAL_WELL_DESIGNED_NORMALIZATION` proof fragment. The certificate claims preserved visible vars, multiplicity,
order, errors, and graph scope under standard SPARQL semantics for the independent well-designed OPTIONAL case.

Rule-19 migration outcome: `LmdbSetSemanticsOptimizer` now emits a generic certificate for duplicate UNION branch
elimination inside a proven set context. The certificate is intentionally not multiplicity-preserving and relies on the
`DUPLICATE_INSENSITIVE_CONTEXT` assumption.

Rule-28 migration outcome: `LmdbSetSemanticsOptimizer` now emits a generic certificate for top-level ASK OPTIONAL
elimination. The certificate is intentionally not multiplicity-preserving and relies on the ASK/existence context where
the OPTIONAL right side is unobservable.

Rule-26 migration outcome: `LmdbSetSemanticsOptimizer` now emits a generic certificate for duplicate LEFT JOIN
idempotence inside a proven set context. The certificate models the rewrite as dead OPTIONAL elimination and records
that bag multiplicity is only safe because downstream duplicates are unobservable.

Rule-5 migration outcome: `LmdbSetSemanticsOptimizer` now emits a generic certificate for duplicate JOIN idempotence
inside a proven set context. The certificate treats this as the duplicate-pattern family, but conservatively records
that multiplicity is only safe because downstream duplicates are unobservable.

Rule-10 migration outcome: `LmdbFilterSimplifierOptimizer` now emits a generic certificate when its existing finite
relation path materializes `FILTER IN` as a `BindingSetAssignment`. The certificate records full safety preservation
under standard SPARQL semantics after the optimizer has proved all condition variables are already assured by the input.

Rule-11 migration outcome: `LmdbFilterSimplifierOptimizer` now emits a generic certificate when its existing finite
relation path materializes an equality disjunction as a `BindingSetAssignment`. The certificate uses the same assured
condition-variable guard as rule 10 and records full safety preservation under standard SPARQL semantics.

Rule-12 implementation outcome: `LmdbFilterSimplifierOptimizer` now recognizes tuple disjunctions shaped as ORs of
equality conjunctions, enumerates the finite tuple relation, and replaces the filter with a `BindingSetAssignment`
guarded by already-assured condition variables. The generated relation carries a rule-12 certificate with full safety
preservation under standard SPARQL semantics.

Rule-13 implementation outcome: `LmdbSetSemanticsOptimizer` now deduplicates duplicate `BindingSetAssignment` rows
only inside duplicate-insensitive set contexts such as `DISTINCT` or the existing ASK-slice context. The generated
certificate records that multiplicity is not preserved and is safe only under `DUPLICATE_INSENSITIVE_CONTEXT`; bag
contexts keep duplicate `VALUES` rows unchanged.

Rule-14 migration outcome: `LmdbFilterSimplifierOptimizer` now emits a generic certificate when independent finite
`VALUES` domains are correlated by `sameTerm` equality and the existing finite relation path materializes the allowed
tuple subset. The certificate records full safety preservation under standard SPARQL semantics; the catalog now marks
rule 14 as partially implemented rather than unsupported.

Rule-15 migration outcome: Cascades now emits a generic certificate when a finite IRI `VALUES` assignment binds the
predicate variable of a variable-predicate `StatementPattern`. The wrapper preserves the existing finite-filter and
standard filter-values-anchor algebra shapes, but adds the rule-15 proof to the selected alternative so provenance is
available even when the standard filter route wins.

Rule-16 implementation outcome: `LmdbCascadesRuleProvider` can now construct a certified alternative for binary
`UNION` branches that are single same-shape `StatementPattern`s differing only by one constant triple position. The
rewrite creates a finite `VALUES` assignment for the constants, generalizes the triple pattern with a helper variable,
and immediately projects back to the original union bindings. Final optimizer selection remains cost-based, so the
focused test checks the alternative builder and certificate directly.

Rule-17 implementation outcome: `StandardCascadesRules` now exposes an exact `Union(Join(P,A), Join(P,B))` to
`Join(P, Union(A,B))` factoring alternative when `P`, `A`, and `B` are pure statement-pattern/finite-binding join
regions and the `UNION` is not a variable-scope boundary. The rule emits a generic rule-17 certificate claiming full
portable safety under standard SPARQL bag semantics. General multi-branch factoring and non-pure graph patterns remain
later slices.

Rule-18 implementation outcome: `StandardCascadesRules` now exposes the symmetric exact `Union(Join(A,P), Join(B,P))`
to `Join(Union(A,B), P)` factoring alternative under the same pure-region and non-scope-changing `UNION` guards as
rule 17. The rule emits a generic rule-18 certificate with full portable safety under standard SPARQL bag semantics.
General multi-branch factoring and non-pure graph patterns remain later slices.

Rule-20 implementation outcome: `StandardCascadesRules` now removes a subsumed filtered `UNION` branch for the narrow
shape `P UNION FILTER(P, BOUND(?v))` when `?v` is assured by `P` and the Cascades goal has set or existence semantics.
The rule emits a generic rule-20 certificate that explicitly records multiplicity is not preserved and requires
`DUPLICATE_INSENSITIVE_CONTEXT`. Bag-semantics goals keep the branch.

Rule-21 migration outcome: the existing `ProjectionUnionDistributionRule` keeps producing the same branch-local
projection alternative for `Projection(Union(A,B))`, but now emits a generic rule-21 certificate under standard SPARQL
bag semantics. This covers the current branch-local projection implementation without widening live-variable analysis.

Rule-22 implementation outcome: `StandardCascadesRules` now offers branch-local `DISTINCT` alternatives for pure,
non-scope-changing `UNION` branches only when the Cascades goal is set or existence semantics. The rule emits a generic
rule-22 certificate that records multiplicity is not preserved and relies on `DUPLICATE_INSENSITIVE_CONTEXT`. Bag
semantics keep UNION branches unchanged.

Rule-23 migration outcome: `LmdbSetSemanticsOptimizer` and `LmdbFilterSimplifierOptimizer` now emit generic
certificates for the existing finite membership semi-filter rewrites used under `COUNT(DISTINCT ?var)`. The
certificates record that multiplicity is not preserved by the early membership-join form and is therefore guarded by
`DUPLICATE_INSENSITIVE_CONTEXT`.

Rule-30 implementation outcome: `StandardCascadesRules` now offers a narrow portable alternative for
`FILTER NOT EXISTS { Q }` when the left side and `Q` are pure graph patterns with a non-empty assured shared key. The
alternative is `MINUS` over `DISTINCT(Project(Q, keys))`, making the anti-join key proof explicit and avoiding the
disjoint-domain mismatch that makes the general NOT EXISTS/MINUS conversion unsafe.

Rule-31 implementation outcome: `StandardCascadesRules` now pushes a local `sameTerm(?v, const)` filter inside
`NOT EXISTS` into the negated statement pattern when `?v` is not mentioned by the outer pattern. The rewrite removes
the inner filter and constantizes the matching statement-pattern position with a generic rule-31 certificate. It
intentionally rejects outer-correlated variables and does not yet handle conjunctions or non-`sameTerm` equality.

Rules-32-through-34 review outcome: RDF4J parser lowering already implements the portable algebra shape for simple
property-path sequence, inverse, and alternation: sequence paths become joins with anonymous path variables, inverse
paths swap statement-pattern endpoints, and alternatives become non-scope-changing `Union` nodes. The rewrite catalog
now marks these families as `DELEGATED_EXISTING` instead of unsupported.

Rule-35 implementation outcome: `StandardCascadesRules` now decomposes simple positive closure
`ArbitraryLengthPath(..., minLength=1)` into one explicit `StatementPattern` step joined with a zero-or-more
`ArbitraryLengthPath(..., minLength=0)` tail, then projects away the generated middle variable. The rule is only
available under set/existence Cascades goals because path multiplicity can change; the certificate records
`DUPLICATE_INSENSITIVE_CONTEXT`.

Rule-40 implementation outcome: `StandardCascadesRules` now lowers `Group` nodes that have grouping keys and no
aggregate elements to `Distinct(Projection(input, groupingKeys))`. The rule rejects aggregate outputs and
scope-changing groups, and emits a generic rule-40 certificate under standard SPARQL semantics.

Rule-41 implementation outcome: RDF4J carries `GRAPH ?g { ... }` scope as a `StatementPattern` context var rather
than as a standalone `Graph` tuple node. `StandardCascadesRules` now folds a top-level
`sameTerm(?g, <graphIRI>)` filter into a `NAMED_CONTEXTS` statement pattern by replacing the unbound context var with
`Var.of("g", graphIRI)`. This preserves the visible `?g` binding while restricting the graph lookup. The rule rejects
non-context variables, literals, already-fixed contexts, and scope-changing filters.

Rule-42 review outcome: RDF4J does not retain repeated fixed `GRAPH` wrappers in tuple algebra. The SPARQL parser sets
the graph context and `NAMED_CONTEXTS` scope before building each `StatementPattern`, so
`GRAPH <g> { A } GRAPH <g> { B }` is already represented as a join of statement patterns with the same fixed context.
The catalog marks rule 42 as delegated to existing parser lowering, with a parser characterization test covering the
shape.

Rule-44 implementation outcome: `StandardCascadesRules` now pushes a projected, deduplicated copy of static local
`BindingSetAssignment` rows into a fixed-IRI `SERVICE` when endpoint metadata explicitly says the endpoint supports
`VALUES`. The rewrite keeps the original local `VALUES` outside the service to preserve local multiplicity, rejects
blank nodes and `UNDEF` rows, updates both the SERVICE child algebra and cached remote expression string, and emits a
generic rule-44 certificate with `FEDERATION_ENDPOINT_COMPATIBLE`.

Rule-45 implementation outcome: `StandardCascadesRules` now pushes a top-level `BOUND(?var)` filter into a fixed-IRI
`SERVICE` when the filter references only variables from the service body and endpoint metadata explicitly reports
compatible expression semantics. The rewrite updates both the SERVICE child algebra and cached remote expression
string, rejects empty metadata and non-remote variables, and emits a generic rule-45 certificate with
`FEDERATION_ENDPOINT_COMPATIBLE`. Broader scalar expressions and local-bound-constant pushdown remain later slices.

Rule-46 implementation outcome: `StandardCascadesRules` now pushes an identity `Projection` over a fixed-IRI `SERVICE`
into the remote service body when endpoint metadata explicitly reports projection support. The rewrite replaces the
outer projection with a `Service` whose child is a projected service expression and whose cached service expression is
a projected `SELECT` subquery. It rejects subquery projections, aliases, projection expressions, variable endpoints,
missing endpoint metadata, and no-op projections, then emits a generic rule-46 certificate with
`FEDERATION_ENDPOINT_COMPATIBLE`. Broader liveness-driven remote projection remains a later slice.

Rule-47 implementation outcome: `StandardCascadesRules` now expands a variable-reference `SERVICE ?endpoint { ... }`
into a deterministic `UNION` of fixed-endpoint `SERVICE` branches when metadata provides a complete endpoint set. Each
branch preserves the visible endpoint binding with an `Extension`, keeps `SERVICE SILENT`, rejects incoming endpoint
correlation and service-body endpoint binding collisions, and emits a generic rule-47 certificate with
`FEDERATION_ENDPOINT_COMPATIBLE`. Expansion from duplicate local endpoint rows remains a later slice.

Rule-48 implementation outcome: `StructuralCascadesRules` now inlines a nested `Projection` marked as a subquery when
the subquery has no solution modifier and an enclosing identity projection continues to enforce the variables visible
above the former subquery boundary. The rule keeps the generic projection merge conservative, rejects projection
contexts and modifier nodes, and emits a generic rule-48 certificate under standard SPARQL semantics. Broader
modifier-aware subquery inlining remains a later slice.

Rule-49 implementation outcome: `StructuralCascadesRules` now narrows a subquery `Projection` to the variables needed
by an enclosing identity projection while keeping the subquery boundary. The rule rejects projection contexts and
solution-modifier inputs such as `DISTINCT`, and emits a generic rule-49 certificate under standard SPARQL semantics.
Broader liveness-driven subquery projection pruning remains a later slice.

Rule-50 implementation outcome: `StructuralCascadesRules` now removes a direct `Order` below a subquery `Projection`
when an enclosing identity projection controls the variables visible above the subquery and the same subquery has no
`Slice`/`LIMIT`/`OFFSET` modifier. The rule keeps the subquery boundary, records that order is not preserved, and emits
a generic rule-50 certificate under standard SPARQL semantics. Top-level order and sliced subquery order remain
rejected.

Rule-24 migration outcome: `LmdbNullRejectingOptionalSupport` now appends a generic certificate to its existing
null-rejecting optional rewrite metric. The certificate covers the `FILTER(OPTIONAL)` to mandatory `JOIN` rewrite under
standard SPARQL semantics and records that visible bindings, multiplicity, order, errors, and graph scope are preserved.

Rule-25 migration outcome: `LmdbSketchJoinOptimizer` now emits a generic certificate for the guarded
`OPTIONAL { Q } FILTER(!BOUND(?v))` to anti-join rewrite when the right-side binding is fresh, assured, shared through
the left, and scope-safe. The certificate is attached to the existing `optimizer.negatedBoundOptionalAlternative`
metric on the selected `Difference` alternative.

Scoped fanout locality outcome: `LmdbNullRejectingOptionalSupport` now preserves locality when a null-rejecting
optional sits over `seed JOIN scopedFanout`. Instead of turning it into a plain `Join(left,right)`, it hoists the
mandatory right side next to the seed when the shared inputs are assured by the seed. This lets the existing finite
anchor and scoped-union distributor duplicate the connected guard into both union branches. The focused red method,
full `LmdbSketchJoinOptimizerTest`, and `LmdbFilterSimplifierOptimizerTest` are green.

## Context and Orientation

The LMDB optimizer pipeline lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. The generic Cascades
optimizer infrastructure lives in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/`.
RDF4J represents parsed SPARQL queries as `TupleExpr` trees. A `QueryOptimizer` mutates a `TupleExpr` before evaluation.
Cascades rules produce alternative `TupleExpr` trees inside a memo search engine.

Important existing classes:

- `LmdbQueryOptimizerPipeline` orders LMDB pipeline optimizers.
- `LmdbFilterSimplifierOptimizer`, `LmdbSetSemanticsOptimizer`, `LmdbSemanticDependencyOptimizer`, and
  `LmdbEligibilitySemiJoinOptimizer` perform existing semantic rewrites.
- `LmdbRewriteProof` stores LMDB-local proof telemetry on rewritten algebra nodes.
- `RuleProof` and `RuleApplication` store Cascades proof metadata.
- `BindingUniverse`, `BindingMask`, and `BindingShape` provide query-local binding identity and assured/possible
  binding facts inside Cascades.
- `ScalarFacts` analyzes scalar expressions in the Cascades IR.

Terms used in this plan:

- Visible binding: a SPARQL variable binding that can be observed by projection, grouping, ordering, filters, joins, or
  `SELECT *`.
- Multiplicity: SPARQL bag semantics, where identical rows can appear more than once.
- Order observable: a context where changing row order can affect visible output, usually `ORDER BY`, `LIMIT`, or
  `OFFSET`.
- Graph scope: the active default or named graph context of a tuple expression.
- Metadata-gated rewrite: a rewrite allowed only when explicit dataset or endpoint metadata proves a schema,
  materialization, graph universe, predicate universe, or federation assumption.

## Current Coverage Review

IDs 1, 2-4, 5, 6, 9, 10-23, 24-28, 30-35, 40-42, 48-50 have partial, delegated, or existing coverage in current LMDB or
Cascades optimizers, but most lack the full certificate dimensions requested by the user. IDs 36-39 and 43-47 need
metadata interfaces before they can be implemented safely. ID 29 must remain unsupported as a general rewrite; only
the narrow key-only `NOT EXISTS` to `MINUS` case from ID 30 should be implemented.

The first migration target is not to duplicate existing behavior. It is to add a shared proof and guard layer, then
move current rewrites onto it with behavior-preserving tests.

## Plan of Work

Milestone 1 adds the safety framework. Create generic types in the Cascades package because that package already owns
binding masks, rule applications, and proof metadata. Add an LMDB adapter so existing `LmdbRewriteProof` metric
fragments can keep working while exposing the richer certificate dimensions.

Milestone 2 adds tests that prove the framework rejects the dangerous cases from the user's spec: lost visible
bindings, multiplicity collapse under `COUNT(*)`, order changes under `LIMIT`, graph-scope loss, `=` versus
`sameTerm` literal confusion, and unproved SERVICE pushdown.

Milestone 3 migrates existing LMDB rewrites without changing their behavior. Existing proof annotations should include
the richer certificate fields. Pipeline tests should assert both old proof kind fragments and new safety fragments
where practical, so downstream diagnostics remain compatible.

Milestone 4 implements missing portable rewrite families in small TDD slices. Each slice adds a focused red test, the
minimal rule, and a green test run. Portable means it depends only on SPARQL algebra semantics, not LMDB schema or
endpoint assumptions.

Milestone 5 implements metadata-gated schema, graph, and federation rewrites. Add metadata interfaces first with empty
defaults that disable all metadata-gated rewrites. Then add one rewrite at a time with tests using explicit in-memory
metadata stubs.

Milestone 6 adds the final rule matrix. The matrix must list all 50 IDs and assert that each rule is implemented,
metadata-gated, already delegated to an existing certified optimizer, or intentionally rejected.

## Concrete Steps

Always work from the repository root:

    /Users/havardottestad/Documents/Programming/rdf4j-small-things

Before tests, the root install command must pass:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

For focused tests, prefer `mvnf`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRewriteCertificateTest#certificateRejectsVisibleBindingLoss --retain-logs

Do not use `-am` or `-q` when running tests.

## Validation and Acceptance

Acceptance for the framework milestone:

- A focused test fails before production code exists.
- The same focused test passes after framework implementation.
- Existing LMDB optimizer pipeline tests still pass.
- Proof telemetry contains both existing readable facts and new certificate dimensions.

Acceptance for the full 50-rule rollout:

- A rule matrix test covers IDs 1 through 50.
- Each implemented rule has at least one positive test and at least one guard/rejection test when the rule has a known
  unsafe variant.
- Metadata-gated rewrites remain disabled with empty metadata.
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` passes.

## Idempotence and Recovery

The framework should be additive. If a rule fails verification, disable only that new rule by removing its registration
or marking its mode unsupported; do not weaken the shared certificate guard. If a test report is overwritten, preserve
the first red or green snippet in `initial-evidence.txt` or a milestone-specific evidence file at the repository root.

Do not delete untracked files. Do not revert user or other-agent changes. If an existing dirty file conflicts with the
framework migration, read the file and preserve its behavior while adding the new proof layer.

## Interfaces and Dependencies

Add these public or package-private interfaces and records in the Cascades optimizer package unless inspection proves a
better existing home:

- `RewriteMode`: `PORTABLE`, `SCHEMA_AWARE`, `FEDERATION_AWARE`, `DUPLICATE_INSENSITIVE`, `AGGRESSIVE`.
- `RewriteAssumption`: string-like value object for standard SPARQL, schema, graph universe, predicate universe,
  federation, and engine-specific assumptions.
- `RewriteSafety`: immutable value for preserved visible vars, multiplicity, order, errors, and graph scope.
- `RewriteCertificate`: rule id, original node id, replacement node id, `RewriteSafety`, and assumptions.
- `RewriteSafetyFacts`: query-local facts needed by guards.
- `RewriteMetadata`: empty-by-default metadata interface for functional properties, inverse-functional properties,
  closures, universes, endpoint support, entailment mode, and freshness.
- `RewriteRuleCatalog`: registry/matrix used by tests to map rule IDs 1 through 50 to implementation status.

Keep dependencies inside the existing RDF4J modules. Do not add third-party libraries for this framework.

## Artifacts and Notes

Initial baseline:

    Command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    Result: BUILD SUCCESS
    Total time: 46.341 s
