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
package org.eclipse.rdf4j.opentelemetry.repository;

import org.eclipse.rdf4j.opentelemetry.OpenTelemetrySupport;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;

/**
 * A {@link Repository} that returns {@link TracingRepositoryConnection}s, so that every query/update evaluated through
 * a connection obtained from it is recorded as an OpenTelemetry span.
 * <p>
 * Wraps an already-constructed, already-initialized {@link Repository} of any kind (local SAIL-backed, remote
 * {@code HTTPRepository}/{@code SPARQLRepository}, federation, ...); it never inspects the delegate's backend, so it
 * composes cleanly with a repository obtained from a {@code RepositoryManager}:
 *
 * <pre>
 * Repository repository = manager.getRepository("my-repo");
 * Repository traced = OpenTelemetrySupport.instrument(repository, "my-repo");
 * </pre>
 *
 * @see OpenTelemetrySupport
 * @author Andreas Schwarte
 * @author Priyadarshan Giri
 */
public class TracingRepository extends RepositoryWrapper {

	private final String repositoryId;
	private final RDF4JOpenTelemetryConfig config;

	/**
	 * @param delegate     the repository to wrap
	 * @param repositoryId recorded as the {@code db.namespace} span attribute
	 * @param config       the OpenTelemetry configuration to use
	 */
	public TracingRepository(Repository delegate, String repositoryId, RDF4JOpenTelemetryConfig config) {
		super(delegate);
		this.repositoryId = repositoryId;
		this.config = config;
	}

	@Override
	public RepositoryConnection getConnection() {
		return new TracingRepositoryConnection(this, repositoryId, config, super.getConnection());
	}
}
