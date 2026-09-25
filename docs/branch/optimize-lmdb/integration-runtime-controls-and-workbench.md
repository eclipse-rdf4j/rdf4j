# Runtime controls, HTTP protocol, and Workbench

This guide covers the runtime-control boundary changed in the historical range
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` → inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. The key operational distinction
is scope: live boolean properties are allowlisted JVM-wide controls on the
server process; a repository selection in Workbench chooses which server/store
is queried but does not make a property repository-local; forced strategy is
attached to one query. The exact property names, defaults, boolean parser
conventions, and consumer sampling times are maintained in
[query configuration](query-configuration.md).

## Runtime property registry

`LmdbRuntimeProperties` is the single fixed registry. It currently enumerates
99 boolean entries grouped for presentation. The controller accepts only names
in that registry and writes the chosen value through `System` properties.
Each property reports its declared default, effective current value, and
whether a value was explicitly set. The JSON wire-state fields are:

```json
{
  "name": "rdf4j.lmdb.example.enabled",
  "group": "Example",
  "label": "Example feature",
  "description": "Short operator-facing description",
  "defaultEnabled": false,
  "enabled": false,
  "explicitlySet": false
}
```

The example is illustrative, not an actual registry entry. A successful
mutation returns one such state object. Use the registry rather than editing a
system property by an undocumented name: parser conventions differ between
legacy `Boolean.parseBoolean`, exact lowercase `false`, and case-insensitive
`false`, and the mutation endpoint deliberately normalizes values to lowercase
`true`/`false`. A property write changes a JVM-wide setting but does not imply
that every active query refreshes it. Some consumers read on each access, some
when constructing a query/operator, and some at dispatch; consult the property
reference for the actual sampling boundary. `hotCountersEnabled` is a special
volatile fast-path gate: it samples the system property when
`LmdbRuntimeProperties` initializes, and the registry's `set()` method updates
it synchronously afterward. A direct `System.setProperty` after class
initialization is not forwarded to that gate and can make `list()` disagree
with the counter fast path. Construction, layout, sizing, filesystem path,
failure-policy, and debug properties are outside the live allowlist.

## Server wire contract

The Spring server registers two JSON resources under the server directory:

| Resource | Method | Success | Contract |
|---|---|---|---|
| `system/lmdb/properties` | `GET` | 200 JSON array | Current allowlisted property states; `Cache-Control: no-store`, UTF-8, `application/json`. |
| `system/lmdb/properties` | `POST` | 200 JSON state object | Form parameters `name` and `enabled`; requires the `rdf4j-admin` role and exact header `X-RDF4J-Admin-Request: true`. |
| `system/lmdb/strategies` | `GET` | 200 JSON array | Forceable strategy `{name, description}` records; no-store, UTF-8, JSON. This is a read-only catalogue. |

For the properties POST, the server returns 403 when either the role or exact
header check fails; 400 with `{"error":"..."}` for an `enabled` value other
than lowercase `true`/`false` or an unknown/non-allowlisted property name; and
200 with the canonical state on success. Unsupported property-resource methods
return 405 and `Allow: GET, POST`; unsupported strategy-resource methods
return 405 and `Allow: GET`. The strategy listing itself excludes the
`NOT_ACTIVATED` sentinel because “no forcing” is not an execution strategy.

The controllers do not enforce an admin role on either GET. That is the
controller contract, not a claim that every deployment exposes unauthenticated
reads: servlet-container constraints, reverse proxies, and application
security can impose additional policy. `X-RDF4J-Admin-Request` is checked
exactly and its non-simple browser request semantics help prevent an ordinary
cross-site form from issuing a mutation; it is not a replacement for role
authorization or deployment CORS policy. The server web descriptor declares
the `rdf4j-admin` role and gives container deployment guidance. Map authorized
accounts to that role in the deployed servlet container.

Sources: [protocol names and locations](../../../core/http/protocol/src/main/java/org/eclipse/rdf4j/http/protocol/Protocol.java),
[property controller](../../../tools/server-spring/src/main/java/org/eclipse/rdf4j/common/webapp/system/LmdbRuntimePropertiesController.java),
[strategy catalogue controller](../../../tools/server-spring/src/main/java/org/eclipse/rdf4j/common/webapp/system/LmdbForceableStrategiesController.java),
[servlet registration](../../../tools/server/src/main/webapp/WEB-INF/common-webapp-system-servlet.xml),
[role declaration and deployment note](../../../tools/server/src/main/webapp/WEB-INF/web.xml).
The source tests are
[`LmdbRuntimePropertiesControllerTest`](../../../tools/server-spring/src/test/java/org/eclipse/rdf4j/common/webapp/system/LmdbRuntimePropertiesControllerTest.java)
and client coverage in `RDF4JProtocolSessionTest`.

## Per-query forced strategy

`lmdb-forced-strategy` is a query request parameter, not a system property.
The protocol treats `null`, blank, and exact `NOT_ACTIVATED` as “do not
force”; the HTTP client sends no parameter for those states. A nonblank value
is passed to `SailQuery` only by the RDF4J Server query handler. Generic
non-Sail operations and non-LMDB Sails do not consume it. The LMDB evaluator
checks the requested name against the forceable catalogue when it constructs
the evaluation strategy and fails before query evaluation with
`QueryEvaluationException` for an unknown name, listing accepted names in the
error. The accepted strategy names are case-sensitive keys; the no-force
sentinel comparison is also exact.

This setting remains local to the query through evaluation/explain. The
Workbench may retain the user's dropdown selection in its query UI/session
state, but it submits the value with each query; that persistence must not be
described as server-global configuration. `HTTPRepository` and
`RDF4JProtocolSession` expose methods to list property/strategy records and
mutate an allowlisted property, while `AbstractHTTPQuery` carries the selected
strategy only for the RDF4J protocol. Plain SPARQL endpoints receive no
RDF4J-specific parameter.

Sources: [server request handoff](../../../tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/handler/DefaultQueryRequestHandler.java),
[Sail query API](../../../core/repository/sail/src/main/java/org/eclipse/rdf4j/repository/sail/SailQuery.java),
[client query parameter](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/query/AbstractHTTPQuery.java),
[HTTP session protocol](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/RDF4JProtocolSession.java),
[repository client](../../../core/repository/http/src/main/java/org/eclipse/rdf4j/repository/http/HTTPRepository.java),
[LMDB validation](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeEvaluationStrategy.java).
The published `LmdbNativeForceableStrategies` catalogue descriptions are also
used in [strategy arbitration and explanation](query-arbitration-and-explanation.md).

## Workbench internal API and UI

The Workbench query servlet exposes these internal query actions:

* `GET /repositories/{id}/query?action=lmdb-properties` returns
  `{"available":true,"properties":[...],"error":null}` on success. A remote
  endpoint that cannot provide the catalogue returns 503 with
  `available:false`, `properties:null`, and an explanatory `error`.
* `GET /repositories/{id}/query?action=lmdb-strategies` has the equivalent
  wrapper fields `available`, `strategies`, and `error`; an unavailable
  catalogue returns 503.
* `POST /repositories/{id}/query` with form `action=set-lmdb-property`,
  `name`, and `enabled` uses the same admin role/header and lowercase boolean
  rule as the server mutation endpoint. It returns 200 with the state object;
  403 for authorization failure; 400 for invalid boolean/name; and 503 when a
  remote server cannot perform the operation. A GET using the mutation action
  returns 405 and `Allow: POST`.

The Workbench sets `Cache-Control: no-store` on these JSON responses. For an
embedded repository it reads or sets the in-process LMDB registry; for an
`HTTPRepository`, it forwards the catalog request to that selected remote
server. Thus the same UI can control an embedded or remote store, but in both
cases the property write changes the LMDB JVM hosting that store. Workbench
source templates and TypeScript build the property table and strategy
dropdown from the server response; an empty strategy choice means no forced
strategy. Query result/explain UI additions show physical explanation and
strategy information. Text explanations also offer normal syntax highlighting,
a hotspot heatmap, and property visibility controls; these change presentation
only. Their data contract is in
[plan/explanation documentation](integration-plan-explanation-and-telemetry.md).

When a strategy is selected, the Workbench reads the form value into
`QueryEvaluator` and applies it to an embedded `SailQuery` or an HTTP query
object. The browser may persist the dropdown choice in Workbench query state,
but the setting applied during each query remains query-scoped rather than
JVM-global. The HTTP query object has a per-query setter and puts the value on
that request, while
`RDF4JProtocolSession.setForcedLmdbExecutionStrategy` is a client-session
setting for subsequent queries through that session; code using the latter
must account for other queries sharing the session. Plain SPARQL-protocol
queries do not carry this RDF4J-specific parameter.

The Workbench's explain JSON response has `format`, `content`,
`strategyDecisions`, and `lineSeparator`. `strategyDecisions` is collected
from the returned generic explanation tree; it can be empty when the selected
explanation level or execution path did not produce decisions. The table
rendering is diagnostic output, not a second planner decision. The related
explanation model and preview/execution distinction are covered in the
[plan guide](integration-plan-explanation-and-telemetry.md).

Workbench result metadata adds `query-duration` in whole milliseconds.
For tuple queries the timer covers `query.evaluate()` and full result
collection into a binding-set list; for graph queries it covers evaluation
and collection into a statement list; boolean queries time the boolean
evaluation. Downloads omit this Workbench metadata. This is not total HTTP
response latency: result formatting/serialization and transport are outside
those timed regions. It adds a small timestamp/metadata value, not a query
result cache, and the existing eager tuple/graph materialization determines
the dominant result memory cost.

## Live query flags versus constructed storage budgets

The same property screen does not expose every LMDB tuning value. Its 99
entries are the JVM-global boolean allowlist described in
[query configuration](query-configuration.md#runtime-property-registry). Store
construction choices such as adjacency enablement and byte budgets live in
`LmdbDirectAdjacencyOptions`; they are captured while the store is constructed
and are not `store.properties` persistence markers. The
`directAdjacency.nodePredicateProjection.serve.enabled` query feature gate is
read at the node-predicate serving boundary; other values captured into store,
query, or operator state do not imply a full active-query refresh after a
property write. The storage guide documents which settings are construction
budgets, their units and AUTO calculation, and which live gate is checked at
use time: [adjacency memory admission and runtime controls](storage-adjacency-lifecycle.md#memory-admission-and-runtime-controls).

Sources: [Workbench servlet](../../../tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/QueryServlet.java),
[query evaluator](../../../tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/QueryEvaluator.java),
[TypeScript query UI](../../../tools/workbench/src/main/webapp/scripts/ts/query.ts),
[Workbench role descriptor](../../../tools/workbench/src/main/webapp/WEB-INF/web.xml).
Relevant coverage is `QueryServletLmdbRuntimePropertyPostTest`,
`QueryServletTest`, and `QueryEvaluatorTest`.

## FAQ for operators and client authors

**Does the property panel change one repository?** No. It addresses the
selected server/process. The server-side runtime registry is static process
state shared by repositories in that JVM.

**Does setting a property rerun or retune an in-flight query?** Not
generically. Each flag's sampling point is listed in
[query configuration](query-configuration.md); only the documented immediate
volatile gate has direct live visibility.

**Can I set any Java system property through the endpoint?** No. Only the
allowlisted names may be changed.

**Is the admin header enough to authorize a write?** No. The server checks
both `rdf4j-admin` and the exact header value.

**Why does an invalid strategy fail at query time rather than the HTTP
handler?** The generic handler transports a string to a Sail-specific API;
LMDB's strategy constructor validates against the store's current catalogue.
The read-only strategy endpoint is the discovery API.
