# Integrate stable LMDB index order with native evaluation

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document is maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

After this work, applications can use `ORDER BY STABLE_INDEX(?variable)` against LMDB to request repeatable backend index order without paying for a separate value sort when the query shape supports it. Existing LMDB native query compilation, sketch join ordering, batched loading, and value-ID optimizations must remain active for queries that do not request stable ordering. DISTINCT and REDUCED fallback evaluation may use bounded, partition-local duplicate tracking when a stable ordered scan is safe, but transaction-local statement changes must disable ordered access so committed index order is never presented as transaction order.

The result is observable through parser tests, optimizer-plan tests, ordered Sail dataset tests, and LMDB repository query tests. The complete `core/sail/lmdb` test suite must pass on the integrated branch.

## Progress

- [x] (2026-07-12 21:24Z) Inspected `optimize-lmdb`, fetched `origin/stable-order`, and mapped the five stable-order commits.
- [x] (2026-07-12 21:24Z) Completed the required root quick clean install; all reactor modules built successfully.
- [x] (2026-07-12 21:24Z) Identified the current native evaluator and custom optimizer pipeline as the integration points.
- [x] (2026-07-12 21:44Z) Captured focused pre-change LMDB/parser evidence and the required failing parser regression.
- [x] (2026-07-12 22:16Z) Ported the parser, sentinel function/model, dataset safeguards, planner, and focused stable-order tests.
- [x] (2026-07-12 22:48Z) Integrated ordered scans into the current LMDB storage and native source structures.
- [x] (2026-07-12 22:55Z) Preserved statement order through native aggregate/BGP compilation and added a zero-copy ordered cursor merge.
- [x] (2026-07-13 00:21Z) Completed formatting, copyright validation, clean reactor install, focused/cross-module tests, exhaustive LMDB verification, transient-failure reruns, and final diff audit.

## Surprises & Discoveries

- Observation: `origin/stable-order` contains five commits and changes 29 files, but its evaluator integration predates `LmdbNativeEvaluationStrategy`.
  Evidence: the branch adds `LmdbEvaluationStrategy` and `LmdbEvaluationStrategyFactory`, while the current branch automatically uses `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeEvaluationStrategy.java` and `LmdbNativeEvaluationStrategyFactory.java`.

- Observation: the current branch already has a custom `LmdbQueryOptimizerPipeline` with reusable stateless optimizers, LMDB filter simplification, and sketch join ordering.
  Evidence: the pipeline ends with `LmdbFilterSimplifierOptimizer`, `LmdbSketchJoinOptimizer`, and `OrderLimitOptimizer`; replacing it with the stable branch's `StandardQueryOptimizerPipeline` delegate would discard current optimizations.

- Observation: the old `SailDatasetTripleSource` no longer exists on the current branch.
  Evidence: evaluation now uses `SailDatasetTripleTermSource`, whose typed `getDatasets(Class<T>)` traversal deliberately refuses non-transparent changeset/observer wrappers. This supplies a reflection-free transaction-safety boundary for native and ordered access.

- Observation: the current LMDB store retains the ordered Sail API stubs but has substantially different read/write internals and native source implementations.
  Evidence: `LmdbSailStore.LmdbSailDataset#getStatements(StatementOrder, ...)` still throws `UnsupportedOperationException`, while the surrounding class contains packed-value, aligned bulk-write, reusable-probe, and parallel-snapshot paths absent from `stable-order`.

- Observation: the native aggregate and BGP compilers accepted rewritten `StatementPattern` nodes but originally discarded their requested `StatementOrder`.
  Evidence: the first POSC regression explain plan showed `StatementPattern [statementOrder: O]` while the physical `NativeRows` pattern still selected `psoc`; passing the order into `PatternPlan` and `NativePattern` changed the physical index to `posc` and made the native query pass.

- Observation: native LMDB record cursors reuse their `long[]` row buffers.
  Evidence: eagerly advancing one source in the first ordered explicit/inferred merge mutated the row just returned to the consumer, producing a duplicated object and dropping the first object. Deferring source advancement until the next `next()` call preserved zero-copy behavior and restored all rows.

- Observation: the exhaustive LMDB verify is expensive and exposed one transient, pre-existing benchmark plan-shape assertion.
  Evidence: 2,098 unit tests passed; 113 of 114 integration tests passed in the 1:05-hour aggregate run. The sole ordinary-query failure in `ThemeQueryBenchmarkSmokeIT` passed both as an isolated method and as the complete 10-test class immediately afterward.

## Decision Log

- Decision: Port the stable-order feature semantically instead of merging the old evaluator and pipeline implementations verbatim.
  Rationale: the current implementations are newer optimization owners. Reintroducing the stable evaluator or replacing the pipeline would silently bypass native compilation and sketch planning.
  Date/Author: 2026-07-12 / Codex

- Decision: Keep transaction-change detection structural and reflection-free.
  Rationale: `SailDatasetTripleTermSource.getDatasets(...)` already treats changeset and observing wrappers as non-transparent, so an empty native-source view is the correct signal that committed LMDB index order is unavailable.
  Date/Author: 2026-07-12 / Codex

- Decision: Preserve the stable branch's focused tests and adapt them only where current package/layout contracts changed.
  Rationale: those tests describe parser validation, join-order safety, DISTINCT correctness, pending-change safeguards, and ordered index behavior that must survive the port.
  Date/Author: 2026-07-12 / Codex

- Decision: Teach both current native compilers and native sources about ordered scans rather than falling back from native evaluation wholesale.
  Rationale: preserving order at `PatternPlan`/`NativePattern` keeps the optimized ID/slot execution, aggregate compilation, join ordering, and packed LMDB access active for supported stable-order queries.
  Date/Author: 2026-07-12 / Codex

- Decision: Merge multiple ordered native cursors with a priority queue and deferred advancement.
  Rationale: the merge is `O(N log k)` for `k` sources/contexts, allocates only one small head per source, and respects reusable row buffers without materializing or copying result rows.
  Date/Author: 2026-07-12 / Codex

## Outcomes & Retrospective

The stable-order branch behavior is integrated into `optimize-lmdb` without reintroducing its obsolete evaluation strategy, factory, reflection-based transaction state, or standard optimizer pipeline. `STABLE_INDEX(?var)` now parses and validates, is consumed only by the LMDB optimizer, selects compatible physical indexes, survives both current native compiler paths, composes across explicit/inferred and multi-context sources, and rejects unsafe transaction-local ordering. DISTINCT/REDUCED use the stable partitioned path where safe.

The optimized branch remains the architecture owner: ordinary queries retain `LmdbNativeEvaluationStrategy`, the native aggregate/BGP compilers, filter simplification, sketch join planning, packed storage, and reusable probes. Ordered native multi-source scans use a priority-queue merge with `O(N log k)` time, `O(k)` merge state, and no per-row copy.

Validation completed with a green clean reactor install; focused parser, dataset, memory, planner, DISTINCT, native store, and pipeline selections; 2,098 green LMDB unit tests; 113/114 integration tests in the exhaustive pass; and green reruns of the sole transient failing IT both alone and as its complete 10-test class. Formatting, copyright checks, `git diff --check`, and conflict/unmerged-file audits are clean.

## Context and Orientation

`core/queryparser/sparql` parses the public `STABLE_INDEX` spelling into a private function IRI because the generated SPARQL grammar does not recognize this store-specific keyword. `core/queryalgebra/model` owns the sentinel IRI, and `core/queryalgebra/evaluation` registers a function that fails if a backend optimizer does not consume the sentinel. This makes unsupported stores fail explicitly rather than accidentally sorting by an arbitrary function result.

`core/sail/base` composes committed datasets, transaction changesets, observation wrappers, and explicit/inferred unions. Ordered reads are only valid when every composed dataset can expose a compatible comparator and has no transaction-local statement changes.

`core/sail/lmdb` owns the ordered index implementation. `TripleStore` selects a physical LMDB index compatible with a requested `StatementOrder`. `LmdbSailStore` converts RDF values to IDs, exposes ordered Sail iterators, and compares values by their stable LMDB IDs. `LmdbQueryOptimizerPipeline` must insert the stable-order optimizer without removing the current filter/sketch optimizers. `LmdbNativeEvaluationStrategy` must retain native query compilation for ordinary queries and use the generic ordered statement path when a stable-order rewrite marks a statement pattern.

## Plan of Work

First capture pre-change test reports from the smallest relevant current LMDB and parser selections. Then bring over the branch's parser/model/function changes and focused tests. Run those tests before storage and evaluator integration so failures identify the missing behavior.

Implement ordered index selection in the current `TripleStore` without changing the packed write paths. Add ordered iterator and comparator support to the current `LmdbSailStore.LmdbSailDataset`. Compose ordered access safely in `SailDatasetImpl`, `ObservingSailDataset`, `UnionSailDataset`, and `SailDatasetTripleTermSource`, using current transparent-wrapper APIs rather than the removed source class.

Place `LmdbOrderByOptimizer` after the current `OrderLimitOptimizer`, retain all existing optimizers, and integrate stable DISTINCT/REDUCED preparation into `LmdbNativeEvaluationStrategy`. The strategy must continue attempting native compilation for unmarked algebra. If the native compiler cannot guarantee a requested physical order, the marked subtree must use the generic evaluator so `StatementPatternQueryEvaluationStep` performs the ordered scan.

Finally run parser, Sail base, memory, and LMDB focused tests; run the full affected module suites; format; repeat the root quick install; and audit the final diff for conflict markers, obsolete evaluator classes, accidental user-artifact changes, and optimization regressions.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j`. Maven commands use `.m2_repo`, never use `-am` with tests, and never use `-q` with tests.

The initial build command was:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

It completed with `BUILD SUCCESS` in 32.210 seconds. Focused tests use:

    python3 .codex/skills/mvnf/scripts/mvnf.py <Class#method> --module <module> --retain-logs

The final module verification uses:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

## Validation and Acceptance

Parsing `SELECT * WHERE { ?s ?p ?o } ORDER BY STABLE_INDEX(?s)` must succeed. Using `STABLE_INDEX` outside a standalone ORDER BY expression or with a non-variable argument must fail during parsing. A non-LMDB evaluator must fail clearly if the sentinel reaches function evaluation.

LMDB must remove a supported ascending stable-order request, mark the matching statement pattern with the physical statement order, and produce repeatable results. It must leave unsupported shapes to the generic sort and reject explicit stable-order requests when transaction-local statement changes make committed index order unsafe. DISTINCT and REDUCED results must remain complete while partition-local duplicate state is cleared only at genuine ordered value boundaries.

Ordinary LMDB queries must still receive the native evaluation strategy and current optimized pipeline. Existing native plan, sketch optimizer, loading, OOM, and transaction tests must remain green.

## Idempotence and Recovery

The branch inspection and test commands are repeatable. Existing modified and untracked planning/profile artifacts belong to the user and must remain untouched. Integration edits are limited to files changed by `origin/stable-order` plus current native evaluator tests needed to prove composition. If a ported test exposes an incompatible old assumption, preserve the behavioral assertion and adapt only its construction/package details.

## Artifacts and Notes

The baseline root build is retained in `maven-build.log`. Compact initial focused test evidence will be written to `initial-evidence.txt`, and retained verify logs will be kept under `logs/mvnf/`.

## Interfaces and Dependencies

No new external dependency is required. `LmdbIndexOrder.FUNCTION_URI` remains the internal sentinel shared by parser, evaluator function, and LMDB optimizer. `LmdbStableOrderPlanner` returns a resolution containing the visible binding, anchor statement pattern, requested order variable, and safe join rewrites. `TripleStore.getTriples(Txn, StatementOrder, ...)` and `TripleStore.getSupportedOrders(...)` expose physical ordered scans. `SailDatasetTripleTermSource` remains the bridge from generic evaluation to composed Sail datasets; no reflection or public API widening is needed.

Revision note (2026-07-12 21:24Z): Created after branch, architecture, and conflict analysis to guide a semantic port onto the optimized LMDB evaluator.
