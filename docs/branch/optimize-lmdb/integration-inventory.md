# `optimize-lmdb` branch feature and coverage inventory

This index records the exact source comparison and maps each changed path
family to a developer guide. The path manifest has one record per changed
path; path count is not feature count. A feature can touch production source,
tests, fixtures, scripts, and reports at once, and one path can support more
than one documented contract.

## Comparison boundary

The historical inventory compares merge base
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` with inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. Source descriptions in this guide
set were checked against current source revision
`a678d9a369deded63520cd86b6c30152485b7f6d`; the branch [README](README.md)
records these revision roles. `origin/develop`
`5eee576f5ad74725852feae859a0f9010dae10ff` is the ref recorded for the
historical comparison, not a claim about the live ref. The committed historical
path list is [changed-paths.tsv](changed-paths.tsv), produced from a diff
between the two explicit commits. It contains 1,786 change records: 1,481 added,
302 modified, one deleted, and two renamed paths. Git's summary reports
2,381,547 insertions and 9,346 deletions.

These read-only commands reproduce the historical ledger and its numeric
summary; they do not overwrite generated output:

```sh
git diff --name-status -M 4aec7e9a2223d873b1c1a7703aad4c87bf8354df a869fe298dc4700ce956bcaf9fece3745c57fe05
git diff --shortstat 4aec7e9a2223d873b1c1a7703aad4c87bf8354df a869fe298dc4700ce956bcaf9fece3745c57fe05
```

The path count is not a feature count. It includes generated parser output,
fixtures, tests, dated benchmark captures, experimental patch files, packaged
scripts, and assistant/developer workflow assets. The manifest and this map
describe only the historical committed range; their path counts do not change
when later source or documentation commits are added. The original inventory
was source-oriented. The current source review revision is identified above;
test names elsewhere remain pointers to coverage contracts, not claims of
passing test runs.

The `core/sail/lmdb` subtree accounts for 1,185 paths. The remaining 601 paths
are assigned below. The table groups every changed path family exactly once;
the feature catalog later in this file makes cross-family destination links
explicit. Within the 467 changed LMDB Java paths, primary documentation
ownership is storage 173 and query 294. Storage is primary for
`estimate/**` (two Java paths) and the native mmap/page lifetime; query
documents how estimation is consumed. `LmdbRuntimeProperties` is query primary,
with this integration guide explaining its HTTP/Workbench scope boundary.
`IndexQuadMatchers` and `LeadingFieldSorters` are storage primary, with query
documentation covering their consumers.

## Path-family coverage map

| Changed path family | Paths | Feature area covered | Guide destination |
|---|---:|---|---|
| `.agent/`, `.codex/`, `.claude/` | 58 | Agent skills and developer workflows for Maven tests, JMH/JFR, plan snapshots, debugging, and source review. `.claude/` is classified by pathname only; its instruction contents are outside this source inventory. | [Developer tooling](integration-developer-tooling.md) |
| `.github/workflows/` | 1 | PR verification adds the SPARQL AST patch consistency check. | [Developer tooling](integration-developer-tooling.md#sparql-ast-patch-workflow); [build and packaging](integration-build-and-packaging.md) |
| `assembly/` | 4 | SDK assembly includes the LMDB bulk-loader module and Unix/Windows launchers; paths to API docs and WARs become configurable. | [Build and packaging](integration-build-and-packaging.md); [bulk loading](integration-bulk-loading.md) |
| `benchmark-results/` | 135 | Dated baseline, comparison, profiling, and tiered captures; measurements remain historical and workload/host-specific. | [Performance evidence](integration-performance-evidence.md#historical-reports-and-source-authority) |
| `compliance/repository/` | 2 | Federation `SERVICE` logical-invocation contract and regression coverage. | [Federation semantics](integration-federation-semantics.md); [compliance map](integration-compliance-and-regression-map.md#federation-semantics-and-multi-store-regressions) |
| `compliance/sparql/` | 8 | LMDB SPARQL compliance wrappers and stored baseline support. | [Compliance map](integration-compliance-and-regression-map.md#lmdb-sparql-compliance-harness); [LMDB store](storage-overview.md) |
| `core/collection-factory/` | 7 | Binding-set compatibility index API and factory implementations used by join evaluation. | [Shared query structures](integration-shared-query-structures.md); [query joins](query-joins-and-factors.md) |
| `core/common/` | 8 | Experimental seek and cooperative timeout/cancellation contracts in closeable iterations. | [Iteration and operation lifecycle](integration-query-lifecycle.md) |
| `core/http/` | 6 | LMDB runtime-property and forced-strategy protocol support; existing cancel transport is lifecycle context. | [Runtime controls](integration-runtime-controls-and-workbench.md); [HTTP cancellation context](integration-query-lifecycle.md#existing-rdf4j-server-http-cancellation-protocol) |
| `core/model-api/` | 2 | Core datatype classification helpers. | [Shared query structures](integration-shared-query-structures.md) |
| `core/model/` | 2 | Dynamic model backing and key-based lookup behavior. | [Shared query structures](integration-shared-query-structures.md) |
| `core/query/` | 5 | Generic plan nodes, explanation context, strategy decisions, and telemetry names. | [Plan explanation and telemetry](integration-plan-explanation-and-telemetry.md) |
| `core/queryalgebra/evaluation/` | 79 | Shared evaluation semantics and planning: join order, binding compatibility, replay/materialization, ORDER BY mode and hints, expression error handling, determinism, paths, and value ordering. | [Shared evaluator semantics](integration-query-evaluation.md); [shared query structures](integration-shared-query-structures.md) |
| `core/queryalgebra/geosparql/` | 11 | Determinism declarations and spatial-function support used by safe optimizer reasoning. | [Shared evaluator semantics](integration-query-evaluation.md#function-repeatability-is-a-semantic-promise) |
| `core/queryalgebra/model/` | 11 | Query-model contracts, `BindingSetAssignment`, reified triple references, and the LMDB index-order hint node. | [Shared query structures](integration-shared-query-structures.md); [shared evaluator semantics](integration-query-evaluation.md) |
| `core/queryparser/sparql/` | 14 | SPARQL AST generation, prefix processing, parser entry points, and tuple-expression construction. The language guide separates the maintained grammar and parser logic from generated JavaCC output; the tooling guide covers how that output is patched reproducibly. | [SPARQL language](integration-sparql-language.md); [developer tooling](integration-developer-tooling.md) |
| `core/queryrender/` | 2 | Comprehensive renderer test corpus is exposed as a lazy categorized query stream and a test JAR for LMDB generated-query coverage and benchmark consumers; no runtime renderer implementation is changed. | [SPARQL language](integration-sparql-language.md); [generated query corpus](integration-performance-evidence.md#shared-generated-query-corpus) |
| `core/repository/` | 15 | Query cancellation and lifecycle through repository APIs, HTTP and Sail repositories, manager wiring, and federated `SERVICE` evaluation. | [Query lifecycle](integration-query-lifecycle.md); [federation semantics](integration-federation-semantics.md) |
| `core/sail/api/` | 6 | Cancellation propagation and connection-level query context contracts. | [Query lifecycle](integration-query-lifecycle.md) |
| `core/sail/base/` | 18 | Dataset, source-branch, changeset, and iteration lifecycle behavior shared by Sails. | [Query lifecycle](integration-query-lifecycle.md); [Sail source lifecycle](integration-sail-source-lifecycle.md) |
| `core/sail/lmdb/` | 1,185 | LMDB value storage, adjacency, ingestion/recovery, native evaluation, planning/statistics/cache, and associated tests and fixtures. | [Storage inventory](storage-inventory.md) and [query inventory](query-inventory.md) map each implementation/test family to the completed [storage guides](README.md#storage-feature-catalog) and [query guides](README.md#native-query-feature-catalog). |
| `core/sail/memory/` | 10 | Cross-store regression tests for query semantics, transaction ingestion, and plan retrieval; these paths are tests, not a MemoryStore implementation change. | [Compliance and regression map](integration-compliance-and-regression-map.md) |
| `core/sail/nativerdf/` | 2 | Cross-store tests for ORDER BY evaluation mode and binding-set compatibility indexing; these paths are tests, not a NativeStore implementation change. | [Compliance and regression map](integration-compliance-and-regression-map.md) |
| `e2e/` | 3 | Browser-level tutorial contracts and query-browser test harness changes. | [Documentation and tutorial assets](integration-doc-assets-and-tutorial.md) |
| `scripts/` | 14 | Bulk-load launcher, AST patch manager, compliance-baseline checker, regret-query scorer, Maven workspace support, benchmark runners, and report helpers. | [Developer tooling](integration-developer-tooling.md); [bulk loading](integration-bulk-loading.md); [performance evidence](integration-performance-evidence.md) |
| `site/content/` | 2 | LMDB Store and Server/Workbench documentation updates. | [Runtime controls and Workbench](integration-runtime-controls-and-workbench.md); [storage overview](storage-overview.md) |
| `site/static/` | 1 | Standalone adjacency tutorial page. | [Documentation and tutorial assets](integration-doc-assets-and-tutorial.md) |
| `testsuites/benchmark-common/` | 9 | Shared benchmark query catalog and Rio/benchmark helpers with associated coverage. | [Performance evidence](integration-performance-evidence.md) |
| `testsuites/benchmark/` | 6 | Benchmark plan/result harness and theme-query workload coverage. | [Performance evidence](integration-performance-evidence.md) |
| `testsuites/sparql/` | 7 | SPARQL parser manifest and query-test suite changes. | [Compliance and regression map](integration-compliance-and-regression-map.md); [SPARQL language](integration-sparql-language.md) |
| `tools/generated-keys/` | 41 | Standalone generated-key benchmark/regression harnesses, patch variants, and reports. Production reachability is documented from `core/sail/lmdb` wiring; the tool-directory contents alone are not treated as runtime features. | [Performance evidence](integration-performance-evidence.md) |
| `tools/lmdb-bulk-load/` | 5 | Standalone bulk-loader CLI, disk-space monitoring, and unit coverage. | [Bulk loading](integration-bulk-loading.md) |
| `tools/lmdb-join/` | 18 | Isolated join implementation experiments, capture/compare harness, and benchmark/regression sources. | [Performance evidence](integration-performance-evidence.md) |
| `tools/lmdb-packed/` | 15 | Packed-I/O and specialization experiments with regression and benchmark harnesses. | [Performance evidence](integration-performance-evidence.md) |
| `tools/lmdb-performance/` | 27 | Standalone LMDB microbenchmark suites and comparison harness. | [Performance evidence](integration-performance-evidence.md) |
| `tools/server-boot/` | 3 | Query timeout/read-handle logging and servlet behavior. | [Query lifecycle](integration-query-lifecycle.md); [runtime controls and Workbench](integration-runtime-controls-and-workbench.md) |
| `tools/server-spring/` | 7 | Server interceptors, query request handling, and LMDB runtime-property/forced-strategy controllers. | [Runtime controls and Workbench](integration-runtime-controls-and-workbench.md) |
| `tools/server/` | 2 | Server servlet registrations for system controls. | [Runtime controls and Workbench](integration-runtime-controls-and-workbench.md) |
| `tools/workbench/` | 21 | Query execution and cancellation, runtime controls, explanations, result templates, and generated JavaScript/TypeScript/CSS/XSL assets. | [Runtime controls and Workbench](integration-runtime-controls-and-workbench.md) |
| `tools/value-overlay/` | 7 | Isolated value-overlay storage audits, regressions, and read benchmarks. | [Performance evidence](integration-performance-evidence.md); [value-overlay architecture](storage-value-overlay.md) |
| `tools/pom.xml` | 1 | Tool-module reactor/build wiring. | [Build and packaging](integration-build-and-packaging.md) |
| Root metadata (`README.md`, `pom.xml`, `.gitattributes`, `.gitignore`, `CLAUDE.md`, deleted `run.sh`) | 6 | Maven version/build profiles, workspace output layout, test memory/temp output settings, runtime dependency management, distribution metadata, and top-level documentation pointer. `CLAUDE.md` is classified by pathname; its instruction contents are outside this source inventory. | [Build and packaging](integration-build-and-packaging.md); [developer tooling](integration-developer-tooling.md) |

The rows sum to 1,786 changed paths. Within the 601 non-LMDB paths, the core
families account for 198 paths; tooling accounts for 147; the remaining 256
paths are benchmark evidence, assistant/developer workflow assets, testsuites,
scripts, compliance, packaging, documentation/tutorials, end-to-end tests, CI,
and root metadata.

## Feature and evidence map

The branch index at [README.md](README.md) is the reader-facing catalog: every
feature row has a destination. The table below keeps changed shared/core
surface families auditable against representative implementation and test
anchors. Test names and fixtures point to source coverage contracts; they do
not establish a current passing result. The exact ownership partition for LMDB implementation Java paths is
in the [storage inventory](storage-inventory.md#historical-source-scope)
and [query inventory](query-inventory.md#query-and-storage-ownership-boundary).

| Feature group | Representative changed source | Representative source coverage | Completed guide |
|---|---|---|---|
| Shared join ordering, function safety, replay joins, strict ordering | [`QueryJoinOptimizer`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryJoinOptimizer.java), [`QueryEvaluationUtility`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/util/QueryEvaluationUtility.java), [`MaterializedReplayJoinIterator`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/MaterializedReplayJoinIterator.java) | `QueryJoinOptimizerConnectivityTest`, `QueryJoinOptimizerMergeJoinBoundVarsTest`, `JoinIndependentOperandTest`, `MaterializedReplayJoinIndexTest`, `FunctionDeterminismTest`, `OrderByQueryEvaluationModeTest` | [Shared evaluator semantics](integration-query-evaluation.md) |
| Binding compatibility, VALUES guarantees, datatype/model caches, reified terms, query-model metadata | [`BindingSetCompatibilityIndex`](../../../core/collection-factory/api/src/main/java/org/eclipse/rdf4j/collection/factory/api/BindingSetCompatibilityIndex.java), [`BindingSetAssignment`](../../../core/queryalgebra/model/src/main/java/org/eclipse/rdf4j/query/algebra/BindingSetAssignment.java), [`DynamicModel`](../../../core/model/src/main/java/org/eclipse/rdf4j/model/impl/DynamicModel.java) | API/MapDB `BindingSetCompatibilityIndexTest`, `BindingSetAssignmentTest`, `DynamicModelTest`, `ReifiedTripleRefBindingNamesTest` | [Shared query structures](integration-shared-query-structures.md) |
| Parser, prefix expansion, expression-valued triple components, generated AST, ordered hint syntax, query rendering | [`PrefixDeclProcessor`](../../../core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/PrefixDeclProcessor.java), [`sparql.jjt`](../../../core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast/sparql.jjt), parser-generated sources, query-renderer serializers | `PrefixDeclProcessorTest`, `SPARQLParserTest`, `TestSparqlTripleTermParser`, query-renderer tests | [SPARQL language and generated grammar](integration-sparql-language.md) and [AST maintenance](integration-developer-tooling.md#sparql-ast-patch-workflow) |
| Shared generated SPARQL query corpus | [`SparqlComprehensiveStreamingValidTest`](../../../core/queryrender/src/test/java/org/eclipse/rdf4j/queryrender/SparqlComprehensiveStreamingValidTest.java) and its [`queryrender` test-JAR configuration](../../../core/queryrender/pom.xml) | Category factories used by renderer tests are composed into the lazy stream consumed by [`LmdbNativeGeneratedQueryCoverageTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeGeneratedQueryCoverageTest.java) and [`LmdbNativeGeneratedCorpusBenchmark`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeGeneratedCorpusBenchmark.java); execution of either consumer is not implied. | [Generated-query corpus and evidence boundaries](integration-performance-evidence.md#shared-generated-query-corpus) |
| Federated SERVICE batching, row-correlated extension, SILENT failure boundary | [`RepositoryFederatedService`](../../../core/repository/sparql/src/main/java/org/eclipse/rdf4j/repository/sparql/federation/RepositoryFederatedService.java), [`ServiceJoinIterator`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/federation/ServiceJoinIterator.java) | `RepositoryFederatedServiceTest`, SPARQL federation compliance cases | [Federation semantics](integration-federation-semantics.md) |
| Sail snapshot leases, compact changeset ingestion, transparent native sources | [`SailSourceBranch`](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailSourceBranch.java), [`Changeset`](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/Changeset.java), [`SailDatasetTripleTermSource`](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailDatasetTripleTermSource.java) | `SailSourceBranchTest`, `ChangesetTest`, `SnapshotSailStoreTest`, `UnionSailDatasetTest` | [Sail source lifecycle](integration-sail-source-lifecycle.md) |
| Iteration seek, cooperative timeout, existing remote request cancellation, iterable add hook | [`CloseableIteration`](../../../core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/CloseableIteration.java), [`TimeLimitIteration`](../../../core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/TimeLimitIteration.java), [`AbstractRepositoryConnection`](../../../core/repository/api/src/main/java/org/eclipse/rdf4j/repository/base/AbstractRepositoryConnection.java) | `CloseableIterationSeekDefaultTest`, `TimeLimitIterationTest`, `CooperativeCancellationWrapperTest`, `RDF4JProtocolSessionTest` | [Query and iteration lifecycle](integration-query-lifecycle.md) |
| Runtime property allowlist, forced strategy protocol, Workbench actions and display | [`LmdbRuntimePropertiesController`](../../../tools/server-spring/src/main/java/org/eclipse/rdf4j/common/webapp/system/LmdbRuntimePropertiesController.java), [`QueryServlet`](../../../tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/QueryServlet.java), [`AbstractHTTPQuery`](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/query/AbstractHTTPQuery.java) | `LmdbRuntimePropertiesControllerTest`, `QueryServletLmdbRuntimePropertyPostTest`, `QueryEvaluatorTest` | [Runtime controls and Workbench](integration-runtime-controls-and-workbench.md) and [query configuration](query-configuration.md) |
| Generic plan metadata, strategy decisions, explanation and counters | [`GenericPlanNode`](../../../core/query/src/main/java/org/eclipse/rdf4j/query/explanation/GenericPlanNode.java), [`QueryExplanationContext`](../../../core/query/src/main/java/org/eclipse/rdf4j/query/explanation/QueryExplanationContext.java), [`StrategyDecision`](../../../core/query/src/main/java/org/eclipse/rdf4j/query/explanation/StrategyDecision.java) | `GenericPlanNodeTest` and LMDB explanation tests | [Plan explanation and telemetry](integration-plan-explanation-and-telemetry.md) |
| Repository discovery resilience and server OutOfMemoryError cleanup | [`LocalRepositoryManager`](../../../core/repository/manager/src/main/java/org/eclipse/rdf4j/repository/manager/LocalRepositoryManager.java), [`LoggingDispatcherServlet`](../../../tools/server-boot/src/main/java/org/eclipse/rdf4j/tools/serverboot/LoggingDispatcherServlet.java) | `LocalRepositoryManagerTest`, `LoggingDispatcherServletTest` | [Repository and server runtime](integration-repository-and-server-runtime.md) |
| Bulk CLI, low-disk monitor, package/SDK launchers, library batch hook | [`LmdbBulkLoad`](../../../tools/lmdb-bulk-load/src/main/java/org/eclipse/rdf4j/tools/lmdb/bulk/LmdbBulkLoad.java), [`DiskSpaceMonitor`](../../../tools/lmdb-bulk-load/src/main/java/org/eclipse/rdf4j/tools/lmdb/bulk/DiskSpaceMonitor.java) | `LmdbBulkLoadTest`, `DiskSpaceMonitorTest` | [Bulk-load CLI](integration-bulk-loading.md); [LMDB recovery](storage-bulk-ingestion-recovery.md) |
| JavaCC AST source maintenance, isolated Maven outputs, developer reports and runners | `scripts/manage-sparql-ast.py`, JavaCC patch files, `.agent`/`.codex` tools, Maven POM/profile wiring | `test_manage_sparql_ast.py`, `test_maven_workspace_pom.py`, developer instructions under `.agent` and `.codex` | [Developer tooling](integration-developer-tooling.md); [build and packaging](integration-build-and-packaging.md) |
| Benchmark query catalog, microbenchmarks, plan snapshots, source experiments, historic captures | `testsuites/benchmark*`, `tools/{generated-keys,lmdb-join,lmdb-packed,lmdb-performance,value-overlay}`, `benchmark-results/` | Benchmark module tests and tool-specific harnesses; reports are separately dated artifacts | [Performance evidence](integration-performance-evidence.md) |
| LMDB SPARQL compliance baseline, cross-store regressions, tutorial assets and browser contract | `compliance/sparql`, `compliance/repository`, `core/sail/{memory,nativerdf}/src/test`, `site/static`, `e2e` | LMDB compliance fixtures/checker, federation tests, tutorial browser unit/spec sources | [Compliance and regression map](integration-compliance-and-regression-map.md); [tutorial and docs](integration-doc-assets-and-tutorial.md) |

## Evidence and scope limitations

The branch documentation separates three evidence types:

* **Current source mechanism**: code at review revision
  `a678d9a369deded63520cd86b6c30152485b7f6d` establishes control flow,
  defaults, values, units, ownership, lifetime, failure handling, and
  configuration sampling.
* **Regression/test source**: test names and fixtures show intended coverage;
  they do not imply a current test result.
* **Historical measurement/report**: captures under `benchmark-results/` and
  research artifacts describe only their recorded host, data, query, command,
  commit, JVM, and measurement interval. They are not evidence of performance
  at the current source review revision unless a report matches that provenance.

Production features and their unchanged prerequisites are called out
separately in the guides. Generated parser output is not an independent syntax
design; MemoryStore and NativeStore paths in this range are regression tests,
not store implementation changes; isolated experiments and reports are not
assumed to be on a normal runtime path. The index is exhaustive by changed
path-family counts and representative feature/source anchors, but the feature
catalog is not a one-to-one map from paths to features. The historical inventory,
current source review, static link checks, and any build/test/benchmark evidence
are separate activities; this guide does not present source links as execution
results.
