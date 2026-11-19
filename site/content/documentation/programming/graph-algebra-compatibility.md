---
title: "GRAPH algebra compatibility"
toc: true
weight: 16
autonumbering: true
---

`QueryParseOptions` lets an application choose the algebra produced for SPARQL `GRAPH` patterns. This is useful when an extension inspects or evaluates query algebra and does not yet understand the explicit `Graph` node.
<!--more-->

## Choosing a mode

The immutable options contain independent settings for blank node reference syntax and GRAPH algebra. `GraphAlgebraMode` has three values:

| Mode | Local preparation behavior |
| --- | --- |
| `AUTO` | Repository preparation first applies the temporary JVM default described below. Otherwise require positive declarations from the selected parser factory and the local connection, wrapper, optimizer, and evaluation-strategy path. If both support `Graph`, select `GRAPH`; otherwise select `LEGACY`. Standalone parsing selects `LEGACY`. See the unnegotiated consumer boundaries below. |
| `LEGACY` | Use the historical representation: named-context statement patterns, with the historical limitations of that representation. |
| `GRAPH` | Keep an explicit `Graph` operator around the body. The caller must ensure that downstream algebra consumers support it. |

`QueryParseOptions.DEFAULT`, `QueryParseOptions.STRICT`, and the existing one-boolean constructor all retain `AUTO`. Strict syntax does not select a GRAPH representation. Store initialization does not change a global parser default.

To select compatibility for subsequent preparations, preserve the connection's other settings:

```java
import org.eclipse.rdf4j.query.QueryParseOptions.GraphAlgebraMode;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public final class LegacyAlgebra {

    private LegacyAlgebra() {
    }

    public static void configure(RepositoryConnection connection) {
        connection.setQueryParseOptions(connection.getQueryParseOptions()
                .withGraphAlgebraMode(GraphAlgebraMode.LEGACY));
    }
}
```

The connection keeps the requested options, so `getQueryParseOptions()` may continue to return `AUTO`. RDF4J's standard preparation paths capture those options once, resolve the algebra mode, and attach the concrete options to the parsed query or update. Later option changes affect later preparations. A prepared operation does not change when a different store starts or another connection changes its settings. Manually constructed algebra and external parser implementations remain responsible for their own policy metadata.

An explicit mode wins without consulting the evaluator's default capability. `GRAPH` is an opt-in, not a check that every extension can evaluate the new node.

## Temporary JVM-wide repository default

The GRAPH compatibility switches are deprecated since 6.2.0 and scheduled for removal in the next major release, when explicit `Graph` algebra will be required. This includes the JVM property below; it is a migration aid, not a permanent server setting.

For a server JVM, add this to its Java startup options to retain historical algebra while updating extensions:

```text
-Dorg.eclipse.rdf4j.query.graphAlgebraMode=LEGACY
```

The accepted names are `AUTO`, `LEGACY`, and `GRAPH` (case-insensitive, with surrounding whitespace ignored). A missing property is equivalent to `AUTO`. Boolean values are not accepted.

For each local repository query or update preparation:

1. An explicit connection mode wins, including a concrete endpoint policy merged onto the connection.
2. If the connection still requests `AUTO`, JVM `LEGACY` selects legacy algebra.
3. JVM `AUTO`, JVM `GRAPH`, or an unset property use the normal parser **and** evaluator capability checks. `GRAPH` deliberately does **not** force an unsupported extension to consume `Graph`; today it has the same safe negotiation behavior as `AUTO`.

Explicit per-connection `GRAPH` remains an opt-in responsibility, as described above. The JVM property does not add any parser or evaluator capability.

The property is read when resolving repository `AUTO`, not when a store or connection is created. Changing it affects subsequent preparations, including preparations on existing connections, but never changes an already prepared query or update. Connections still report their requested `AUTO` options. Invalid values, including blank values and `true`/`false`, cause an `IllegalArgumentException` at `AUTO` preparation with the property name and supported values. An explicit connection mode does not read the property.

This default applies to Sail repository preparation (including LMDB and server query/update handlers) and FedX query-plan preparation. It does not change standalone parser defaults, capability-only internal inferencer parsing, manually supplied algebra, or client-side HTTP/SPARQL text passthrough. For remote repositories set the property in the JVM that performs local server-side preparation, not just in the client JVM.

## Standalone parsers and internal consumers

An unconfigured standalone SPARQL parser uses historical algebra. Code that directly parses a query for a known Graph-capable evaluator should pass the concrete mode:

```java
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryParseOptions;
import org.eclipse.rdf4j.query.QueryParseOptions.GraphAlgebraMode;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;

public final class GraphAlgebraParser {

    private GraphAlgebraParser() {
    }

    public static ParsedQuery parse(String query, String baseURI) {
        QueryParseOptions options = QueryParseOptions.DEFAULT
                .withGraphAlgebraMode(GraphAlgebraMode.GRAPH);
        return QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, baseURI, options);
    }
}
```

Distinguish validation from execution. A preliminary parse used only to validate syntax or determine the query type need not select the backend's algebra. It must not copy its standalone LEGACY resolution onto the connection that subsequently prepares the original text.

A parsed-query cache shared by different evaluators must include the effective options in its key. Resolve AUTO before looking up the cache: identical text may need different algebra for different evaluators. If parser providers can be replaced, include the provider's identity and use that same provider to populate the entry. A cache dedicated to a fixed concrete policy and provider can retain a text-only key if those invariants are explicit. Mutable algebra still requires the cache's normal clone or ownership rules.

The built-in custom graph inferencer and SHACL execution paths resolve internal parsing against their actual connections. A query rendered back to text loses its local policy unless the next preparation explicitly receives that policy. Mode selection is not encoded into SPARQL text.

`SpinParser` accepts immutable options through its constructor. They apply consistently to `sp:text` and RDF-form query descriptions, and remain fixed for the lifetime of that parser's template cache. Its existing constructors keep the standalone LEGACY default. Callers selecting GRAPH must provide the options explicitly.

## What LEGACY preserves

LEGACY restores historical GRAPH lowering, rather than deleting a modern `Graph` wrapper. Deleting the wrapper is incorrect because the modern body's statement patterns do not necessarily carry the named graph context.

Compatibility includes the old representation's limitations. For example, with two populated named graphs:

| Pattern | GRAPH | LEGACY |
| --- | --- | --- |
| `GRAPH ?g {}` | Enumerates the two graphs and binds `?g`. | Produces one solution with `?g` unbound. |
| `GRAPH <urn:missing> { BIND(1 AS ?x) }` | Produces no solution when that graph does not exist. | Can produce `?x = 1` because no statement pattern checks graph membership. |
| A subquery within `GRAPH ?g` that does not project `?g` | Retains the outer graph selector. | Can lose the graph binding at the subquery projection. |

The mode also applies to GRAPH patterns in an update's WHERE clause. INSERT and DELETE quad templates continue to represent their contexts on statement patterns. Consequently, an update whose WHERE uses only an empty GRAPH pattern can intentionally produce different writes in LEGACY and GRAPH.

LEGACY does not emulate every behavior of an older RDF4J release. Independent parser and evaluator fixes remain in effect, including SERVICE scope corrections. Do not rely on identical private variable names, optimizer decisions, or tree formatting across releases.

## Extension and wrapper capability

`QueryParserFactory.supportsGraphAlgebra()`, `SailConnection.supportsGraphAlgebra()`, and `EvaluationStrategyFactory.supportsGraphAlgebra()` default to `false`. An extension may override its declaration after auditing the entire path that consumes the algebra: parser hooks, wrappers, query optimizers, and evaluation strategies. A capable evaluator alone is insufficient when the registered parser only implements the older parsing interface.

Known built-in connections and factories opt in explicitly. An unknown subclass or an injected optimizer pipeline does not automatically inherit support merely because its superclass supports Graph. Transparent wrappers forward support only when their own implementation has been audited. A wrapper must not bypass an unknown delegate to find a capable store underneath it.

The check concerns preservation of GRAPH semantics as well as recognition of the node class. Lucene keeps AUTO on LEGACY: its search rewriting does not yet preserve the active graph across the complete search path. A multi-graph regression probe showed a search hit repeated under the wrong graph with explicit GRAPH. Supporting that path requires scoped search evaluation, including custom indexes and geospatial rewriting. A positive capability declaration must wait for that work; explicit GRAPH remains a caller opt-in with this limitation.

An arbitrary `RepositoryConnectionWrapper` can inspect algebra after a built-in repository returns a prepared query. Such an external consumer is invisible to Sail capability negotiation. It must explicitly request LEGACY if it cannot handle `Graph`.

Custom `FederatedServiceResolver` and `FederatedService` implementations are another unnegotiated boundary. A built-in evaluator can pass a `Service` algebra object containing `Graph` to a custom provider under AUTO. The provider does not receive a separate capability check. Applications using an older SERVICE provider that inspects algebra must explicitly select LEGACY, or provide an evaluation factory that conservatively declines GRAPH support. A universal automatic guarantee would require a separate resolver/provider capability contract, including registered providers and their configuration lifetime.

The default therefore protects standalone parsers and the negotiated evaluation components; it does not automatically discover every external algebra consumer.

Existing parser and Sail preparation overloads continue to be used for default syntax with AUTO or effective LEGACY. Explicit GRAPH and unsupported strict syntax must not silently fall through to an old provider that cannot honor them.

### Evaluator configuration lifetime

Capturing parsing options freezes the representation of an operation; it does not freeze a parser registry, evaluation factory, or optimizer pipeline. Configure parser registrations, custom evaluators, and optimizers before preparing operations. If configuration changes later, keep the new path compatible with all retained prepared operations, or stop using those operations and prepare them again after the change. This also applies to wrapper-specific evaluation modes and search interpreters.

This requirement applies to the same store's mutable configuration. Starting a different repository does not alter an operation's policy. A built-in adaptive choice between compatible evaluators is also distinct from installing an incompatible extension.

## Server, Workbench, and remote repositories

RDF4J Server merges endpoint policy with the connection's requested policy before preparation. Endpoint blank node syntax is applied independently of algebra. The graph-mode precedence is:

| Endpoint mode | Connection mode | Mode passed to preparation |
| --- | --- | --- |
| AUTO | AUTO | AUTO, to be resolved by the backend |
| AUTO | LEGACY or GRAPH | The connection's explicit mode |
| LEGACY or GRAPH | Any | The endpoint's explicit mode |

This applies to query requests, SPARQL update bodies and forms, legacy XML transaction requests, and dedicated transaction workers. Workbench prepares the original query or update through its repository. Its graph explorer's preliminary standalone parse does not determine the backend mode.

`HTTPRepository` and `SPARQLRepository` send text to another evaluator. This option adds no HTTP parameter, header, or protocol negotiation. These repositories retain their existing rejection of unsupported nondefault parsing policies. Configure the evaluating server to choose its algebra.

FedX supports local GRAPH evaluation, but it can delegate an original SELECT query to a member. That member prepares the text using its own mode. LEGACY on a FedX connection therefore selects the local algebra without promising historical results across every text delegation boundary. FedX evaluates update WHERE algebra before its write strategy applies the resulting changes; whole-query single-source delegation excludes updates. SERVICE or member subqueries may still execute remotely.

## Java API migration

The existing `QueryParseOptions(boolean)` constructor and `blankNodeReferences()` accessor remain available. Old serialized options that lack the new mode resolve that missing field to AUTO.

The record now has two components. Code that deconstructs it with a one-component record pattern must be updated when recompiling:

```java
case QueryParseOptions(boolean references, var graphMode) -> {
    // Handle both independent policy settings.
}
```

Ordinary constructor and accessor compatibility does not imply source compatibility for record deconstruction. Prefer the named accessors when only one policy field is relevant.

## Storage follow-up identified during validation

The compatibility tests exposed a separate transactional visibility issue in existing GRAPH evaluation. After clearing a named graph within a NativeStore or LMDB transaction, `GRAPH ?g {}` with inferred statements enabled can still enumerate that graph before commit. The explicit-only cases pass. The analogous MemoryStore issue is corrected by applying its existing explicit/inferred dataset filter to context lookup.

NativeStore and LMDB need a storage-level solution that preserves efficient context enumeration. Their current context catalogs combine explicit and inferred data; scanning statement indexes separately for every context could cause substantial extra work. This issue is independent of AUTO resolution and remains a release follow-up for transactional GRAPH correctness.
