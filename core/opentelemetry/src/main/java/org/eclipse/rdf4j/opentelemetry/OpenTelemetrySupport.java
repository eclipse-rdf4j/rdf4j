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
package org.eclipse.rdf4j.opentelemetry;

import java.util.Objects;

import org.eclipse.rdf4j.opentelemetry.repository.DbOtelAttributes;
import org.eclipse.rdf4j.opentelemetry.repository.TracingRepository;
import org.eclipse.rdf4j.repository.Repository;

/**
 * One-line opt-in entry point to enable OpenTelemetry tracing on RDF4J repositories.
 * <p>
 * {@link #instrument(Repository, String)} ({@link org.eclipse.rdf4j.opentelemetry.repository}) traces queries/updates
 * at the {@code RepositoryConnection} level, following OpenTelemetry's database client semantic conventions. This works
 * for any {@link Repository} regardless of backend (local SAIL, remote {@code HTTPRepository}/{@code SPARQLRepository},
 * federation, ...), and composes cleanly with a repository obtained from a {@code RepositoryManager}: wrap the
 * {@link Repository} instance once, after retrieving it.
 * <p>
 * Enabling tracing has no effect on request/response behavior. When no OpenTelemetry SDK is configured (i.e. the
 * default no-op {@code OpenTelemetry} implementation is in effect), the instrumentation adds negligible overhead and
 * emits no data.
 *
 * <pre>
 * Repository managed = manager.getRepository("my-repo");
 * Repository traced = OpenTelemetrySupport.instrument(managed, "my-repo");
 * </pre>
 *
 * For more control (a specific {@code OpenTelemetry} instance, capturing query text, ...), build a
 * {@link RDF4JOpenTelemetryConfig} and pass it to {@link #instrument(Repository, String, RDF4JOpenTelemetryConfig)}, or
 * use {@link TracingRepository} directly.
 */
public final class OpenTelemetrySupport {

	private OpenTelemetrySupport() {
	}

	/**
	 * Wraps the given repository so that every query/update evaluated through a connection obtained from the result is
	 * recorded as an OpenTelemetry span, using {@link RDF4JOpenTelemetryConfig#defaultConfig()}, following the
	 * OpenTelemetry database client semantic conventions ({@link DbOtelAttributes}). Works for any {@link Repository},
	 * regardless of backend.
	 * <p>
	 * The given repository itself is left untouched; use the returned wrapper from this point on.
	 *
	 * @param repository   the (already initialized) repository to wrap
	 * @param repositoryId an identifier recorded as the {@code db.namespace} span attribute, e.g. the id the repository
	 *                     is known by in a {@code RepositoryManager}
	 * @return a {@link Repository} wrapping {@code repository} with tracing enabled
	 */
	public static Repository instrument(Repository repository, String repositoryId) {
		return instrument(repository, repositoryId, RDF4JOpenTelemetryConfig.defaultConfig());
	}

	/**
	 * Wraps the given repository so that every query/update evaluated through a connection obtained from the result is
	 * recorded as an OpenTelemetry span, following the OpenTelemetry database client semantic conventions
	 * ({@link DbOtelAttributes}). Works for any {@link Repository}, regardless of backend.
	 * <p>
	 * The given repository itself is left untouched; use the returned wrapper from this point on.
	 *
	 * @param repository   the (already initialized) repository to wrap
	 * @param repositoryId an identifier recorded as the {@code db.namespace} span attribute, e.g. the id the repository
	 *                     is known by in a {@code RepositoryManager}
	 * @param config       the OpenTelemetry configuration to use
	 * @return a {@link Repository} wrapping {@code repository} with tracing enabled
	 */
	public static Repository instrument(Repository repository, String repositoryId, RDF4JOpenTelemetryConfig config) {
		Objects.requireNonNull(repository, "repository must not be null");
		Objects.requireNonNull(repositoryId, "repositoryId must not be null");
		Objects.requireNonNull(config, "config must not be null");
		return new TracingRepository(repository, repositoryId, config);
	}
}
