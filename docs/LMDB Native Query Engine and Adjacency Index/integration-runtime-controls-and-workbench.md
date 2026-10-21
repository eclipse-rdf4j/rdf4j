# Query strategies, HTTP protocol, and Workbench

Workbench exposes a per-query strategy selector only when the selected repository runs the LMDB native query engine. JVM system properties are deployment settings: Workbench and RDF4J Server provide no property catalog or mutation endpoint.

## Repository strategy discovery

`GET /repositories/{id}/lmdb/strategies` returns a JSON array of `{name, description}` records with `Cache-Control: no-store`. Discovery uses the repository resolved by the normal repository interceptor. Unsupported repositories return an empty array. Repository and Sail wrappers are traversed to find the effective underlying store; repository names do not determine capabilities.

For LMDB, availability follows the store's selected evaluation factory and the existing `rdf4j.lmdb.nativeQueryEngine.enabled` gate. An explicitly configured generic factory disables the selector. A native factory does not advertise strategies when its runtime engine gate is disabled. The catalog describes forceable names; each query still passes the engine's shape and eligibility checks.

The HTTP client and `HTTPRepository` request discovery for their own repository URL, credentials, and additional headers. Local Workbench repositories use the same Sail capability check. The former global `/system/lmdb/properties` and `/system/lmdb/strategies` routes are removed.

The repository resource returns UTF-8 `application/json`, supports only GET, and returns 405 with `Allow: GET` for other methods. Its list excludes `NOT_ACTIVATED`, since absence of forcing is not an execution strategy. Normal repository access rules apply, including any servlet-container or proxy authentication policy.

Workbench's internal `GET /repositories/{id}/query?action=lmdb-strategies` wraps discovery as `{"available":true,"strategies":[...],"error":null}` with `Cache-Control: no-store`. An authoritatively unsupported repository returns HTTP 200 with `available:false`, `strategies:[]`, and `error:null`. A remote discovery failure returns HTTP 503 with `available:false`, `strategies:null`, and an explanatory `error`. Both states keep the selector hidden and disabled, but only the latter is a discovery error.

## Per-query forcing

`lmdb-forced-strategy` is a query request parameter. A missing value, blank string, or `NOT_ACTIVATED` leaves strategy selection to the store. A nonempty supported request is forwarded by the HTTP query session and applied to Sail query execution and explanation, including transactional requests. Direct Server query forcing retains its existing validation and execution semantics.

Workbench starts the selector hidden and disabled while repository discovery is pending. It exposes the selector only after a nonempty catalog arrives. Unsupported repositories and discovery failures clear the selection. Discovery responses belong to their original page and repository, so navigation cannot apply an old response to a newly selected repository.

Workbench also checks selected-repository capability before applying an actual requested or remembered strategy. This prevents stale cookies or manually submitted values from forcing a strategy on an unsupported repository. Ordinary queries with no selected strategy do not perform a catalog request before execution. A discovery failure for an actual forced query propagates as a query error instead of silently executing with a different strategy.

`SailQuery.setForcedLmdbExecutionStrategy` and `AbstractHTTPQuery.setForcedLmdbExecutionStrategy` apply to one query. `RDF4JProtocolSession.setForcedLmdbExecutionStrategy` is a client-session convenience setting for subsequent queries through that session; callers must account for other queries sharing it. Plain SPARQL-protocol queries do not carry this RDF4J-specific parameter. Accepted strategy names and the `NOT_ACTIVATED` sentinel are case-sensitive. LMDB rejects an unknown name with `QueryEvaluationException` before evaluation and lists the accepted names; a known name must still pass the query's eligibility checks.

Sources: [server request handoff](../../../tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/handler/DefaultQueryRequestHandler.java), [Sail query API](../../../core/repository/sail/src/main/java/org/eclipse/rdf4j/repository/sail/SailQuery.java), [HTTP query API](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/query/AbstractHTTPQuery.java), [HTTP session](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/RDF4JProtocolSession.java), [HTTP repository](../../../core/repository/http/src/main/java/org/eclipse/rdf4j/repository/http/HTTPRepository.java), and [LMDB validation](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeEvaluationStrategy.java).

## Explanation and query timing

The Workbench explain JSON response retains `format`, `content`, `strategyDecisions`, and `lineSeparator`. Decisions are collected from the returned generic explanation tree and may be empty when a level or path produces none. The diagnostic table does not make another planner decision. Optimized explanations preview decisions without executing or training; Executed and Timed explanations may report actual routes and metrics. Text syntax highlighting, hotspot heatmaps, and explanation-property visibility affect presentation only. See [plan explanation and telemetry](integration-plan-explanation-and-telemetry.md) and [strategy arbitration and explanation](query-arbitration-and-explanation.md).

Workbench's eager result path retains `query-duration` in whole milliseconds: tuple queries time `query.evaluate()` and collection into binding sets, graph queries time evaluation and collection into statements, and boolean queries time boolean evaluation. Downloads omit this Workbench metadata. Formatting, serialization, and transport lie outside these timed regions, so the value is not total HTTP response latency. It adds timing metadata rather than a result cache; eager tuple/graph materialization still determines that path's dominant result memory cost. Streamed batch progress has its separate elapsed-time contract in [query lifecycle](integration-query-lifecycle.md).

Sources: [Workbench servlet](../../../tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/QueryServlet.java), [query evaluator](../../../tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/QueryEvaluator.java), [query TypeScript](../../../tools/workbench/src/main/webapp/scripts/ts/query.ts), [repository strategy controller](../../../tools/server-spring/src/main/java/org/eclipse/rdf4j/common/webapp/system/LmdbForceableStrategiesController.java), [protocol names and locations](../../../core/http/protocol/src/main/java/org/eclipse/rdf4j/http/protocol/Protocol.java), and [repository route registration](../../../tools/server/src/main/webapp/WEB-INF/rdf4j-http-server-servlet.xml).

## JVM configuration

Configure engine properties in the deployment's JVM configuration. Read boundaries remain specific to each consumer: some values are captured during store or evaluator construction, while others are sampled at compilation or query access. Existing property reads and defaults remain in place. See [query configuration](query-configuration.md) and [per-store accelerator selection](query-configuration.md#per-store-accelerator-selection).

`LmdbRuntimeProperties` retains only internal helpers: `hotCountersEnabled()` reads a cached startup choice on the hot path, and `adjacencySynopsisEnabled()` samples its existing property at the query-access boundary. It has no catalog or mutator.

Construction choices such as adjacency enablement and byte budgets are captured by `LmdbDirectAdjacencyOptions` when the store is constructed; they are not `store.properties` persistence markers. The `directAdjacency.nodePredicateProjection.serve.enabled` query gate is sampled at serving time. Neither it nor another query flag rebuilds an active store or rewrites a captured budget. See [adjacency memory admission and runtime controls](storage-adjacency-lifecycle.md#memory-admission-and-runtime-controls).

## Verification

Relevant coverage includes `QueryServletLmdbAvailabilityTest`, `QueryEvaluatorTest`, `LmdbForceableStrategiesControllerTest`, `RDF4JProtocolSessionTest`, and `Rdf4jServerWorkbenchApplicationTest.removedGlobalLmdbRoutesAreUnavailable`. Rendered QA should cover MemoryStore, LMDB with native evaluation disabled, native-enabled LMDB, wrapped Sail repositories, remembered strategy cookies, discovery failure, and navigation during discovery.
