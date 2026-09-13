/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

/**
 * Opt-in <a href="https://opentelemetry.io/">OpenTelemetry</a> instrumentation for RDF4J.
 * <p>
 * This root package holds the pieces shared across instrumentation areas: the
 * {@link org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig} configuration object and the
 * {@link org.eclipse.rdf4j.opentelemetry.OpenTelemetrySupport} facade, the main public entry point. Currently
 * {@link org.eclipse.rdf4j.opentelemetry.OpenTelemetrySupport#instrument(org.eclipse.rdf4j.repository.Repository, String)}
 * only exposes {@link org.eclipse.rdf4j.opentelemetry.repository}-level tracing (queries/updates at the
 * {@code RepositoryConnection} level, following the OpenTelemetry database client semantic conventions;
 * backend-agnostic and composes cleanly with a repository obtained from a {@code RepositoryManager}).
 * <p>
 * {@link org.eclipse.rdf4j.opentelemetry.http} (outbound SPARQL Protocol HTTP requests, one span per physical HTTP
 * request; only applicable to {@code SPARQLRepository}/{@code HTTPRepository}) also exists and can be used directly
 * (e.g. {@link org.eclipse.rdf4j.opentelemetry.http.TracingHttpClientSessionManager}), but is not currently wired into
 * the {@code OpenTelemetrySupport} facade.
 * <p>
 * This module has no effect unless explicitly enabled, either via
 * {@link org.eclipse.rdf4j.opentelemetry.OpenTelemetrySupport#instrument(org.eclipse.rdf4j.repository.Repository, String)}
 * (one-line opt-in for any {@link org.eclipse.rdf4j.repository.Repository}) or by directly wrapping a
 * {@link org.eclipse.rdf4j.repository.Repository} in a
 * {@link org.eclipse.rdf4j.opentelemetry.repository.TracingRepository}.
 */
package org.eclipse.rdf4j.opentelemetry;
