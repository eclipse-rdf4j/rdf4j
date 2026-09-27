---
title: "Observability using OpenTelemetry"
weight: 10
toc: true
autonumbering: true
---
RDF4J provides opt-in [OpenTelemetry](https://opentelemetry.io/) tracing instrumentation, allowing you to trace SPARQL query/update evaluation (and, for HTTP-backed repositories, the underlying SPARQL Protocol HTTP requests) as part of a distributed trace.
<!--more-->

## Overview

Tracing support is provided by the `rdf4j-opentelemetry` module, and is entirely opt-in: unless explicitly enabled, it has no effect at all - no spans are created, and no OpenTelemetry classes beyond the (dependency-only) [OpenTelemetry API](https://github.com/open-telemetry/opentelemetry-java) are ever touched at runtime.

The module provides two independent instrumentation areas:

- Repository-level tracing (package `org.eclipse.rdf4j.opentelemetry.repository`): one span per SPARQL query/update evaluated through a `RepositoryConnection`, following the [OpenTelemetry database client semantic conventions](https://opentelemetry.io/docs/specs/semconv/database/database-spans/) (`db.system.name`, `db.operation.name`, `db.namespace`, `db.query.text`, `db.response.returned_rows`, ...). This is backend-agnostic: it works identically for a local SAIL-backed repository, a remote `HTTPRepository`/`SPARQLRepository`, or a federation.
- Outbound HTTP tracing (package `org.eclipse.rdf4j.opentelemetry.http`): one span per physical SPARQL Protocol HTTP request, following the OpenTelemetry HTTP client semantic conventions. Only applicable to `SPARQLRepository`/`HTTPRepository`.

For most use cases, repository-level tracing is the more useful of the two, since it works regardless of the repository's backend and composes cleanly with a repository obtained from a `RepositoryManager`. This is also the area exposed by the `OpenTelemetrySupport` facade described below.

If you are looking specifically for how to enable tracing in RDF4J Server/Workbench (including the Docker images), see [RDF4J Server and Workbench](/documentation/tools/server-workbench/#opentelemetry-tracing).

## Programmatic use

The `OpenTelemetrySupport` facade is the one-line opt-in entry point for repository-level tracing:

```java
Repository repository = ...;
Repository traced = OpenTelemetrySupport.instrument(repository, "my-repo");
```

This wraps an already-constructed, already-initialized `Repository` of any kind. It never inspects the delegate's backend, so it composes cleanly with a repository obtained from a `RepositoryManager`:

```java
Repository repository = manager.getRepository("my-repo");
Repository traced = OpenTelemetrySupport.instrument(repository, "my-repo");
```

By default, this uses `GlobalOpenTelemetry.get()` to obtain the `Tracer` used to create spans - i.e. whatever OpenTelemetry SDK/exporter your application (or the OpenTelemetry Java agent, if attached) has configured globally. If your application manages its own `OpenTelemetry` instance instead, pass an explicit `RDF4JOpenTelemetryConfig`:

```java
RDF4JOpenTelemetryConfig config = RDF4JOpenTelemetryConfig.builder()
        .openTelemetry(myOpenTelemetryInstance)
        .captureQueryText(true)
        .build();
Repository traced = OpenTelemetrySupport.instrument(repository, "my-repo", config);
```

## Configuration

`RDF4JOpenTelemetryConfig` (built via `RDF4JOpenTelemetryConfig.builder()`) controls the behaviour of repository-level tracing:

| Builder method                    | System property                                            | Default  | Effect                                                                                     |
|------------------------------------|-------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------|
| `openTelemetry(OpenTelemetry)`     | -                                                            | `GlobalOpenTelemetry.get()` | The `OpenTelemetry` instance used to create tracers and propagators.               |
| `captureQueryText(boolean)`        | `org.eclipse.rdf4j.opentelemetry.captureQueryText`           | `false`  | Records the (possibly truncated) SPARQL query/update text as a span attribute. Disabled by default, since query text can be large and may contain sensitive literal values. |
| `maxQueryTextLength(int)`          | `org.eclipse.rdf4j.opentelemetry.maxQueryTextLength`          | `1000`   | Maximum number of characters recorded for the query/update text attribute, when the above is enabled. |
| `dbSystemName(String)`             | `org.eclipse.rdf4j.opentelemetry.dbSystemName`                | `rdf4j`  | The `db.system.name` span attribute recorded for repository-level spans.                     |

The system properties are read once, as the default value used unless overridden programmatically via the corresponding builder method - this lets deployments that cannot set up an `RDF4JOpenTelemetryConfig` directly (such as RDF4J Server, see below) still configure this behaviour.

## Instrumenting every repository from a RepositoryManager

Rather than instrumenting individual `Repository` instances by hand, `TracingLocalRepositoryManager`/`TracingRemoteRepositoryManager` (package `org.eclipse.rdf4j.opentelemetry.repository.manager`) are drop-in subclasses of `LocalRepositoryManager`/`RemoteRepositoryManager` that instrument every repository they create, gated by the `org.eclipse.rdf4j.opentelemetry.enabled` system property (disabled by default):

```java
RepositoryManager manager = new TracingLocalRepositoryManager(baseDir);
manager.init();
```

Because `RepositoryManager.getRepository(String)` caches its result by id and only ever calls `createRepository(String)` once per id, wrapping there instruments every repository the manager serves exactly once, regardless of which application is using the manager. This is the mechanism RDF4J Server uses - see [RDF4J Server and Workbench](/documentation/tools/server-workbench/#opentelemetry-tracing) for how to enable it there.
