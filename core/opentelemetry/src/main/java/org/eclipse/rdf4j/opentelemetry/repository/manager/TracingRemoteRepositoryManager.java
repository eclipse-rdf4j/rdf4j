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
package org.eclipse.rdf4j.opentelemetry.repository.manager;

import java.util.Objects;

import org.eclipse.rdf4j.opentelemetry.OpenTelemetrySupport;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;

/**
 * A {@link RemoteRepositoryManager} that instruments every repository it creates with OpenTelemetry tracing (see
 * {@link org.eclipse.rdf4j.opentelemetry.repository}) whenever {@link OpenTelemetrySettings#isEnabled()} is
 * {@code true}; otherwise behaves exactly like its superclass.
 */
public class TracingRemoteRepositoryManager extends RemoteRepositoryManager {

	private final RDF4JOpenTelemetryConfig config;

	/**
	 * Creates a manager that, when enabled, instruments with {@link RDF4JOpenTelemetryConfig#defaultConfig()} (i.e.
	 * {@link io.opentelemetry.api.GlobalOpenTelemetry#get()}).
	 */
	public TracingRemoteRepositoryManager(String serverURL) {
		this(serverURL, RDF4JOpenTelemetryConfig.defaultConfig());
	}

	/**
	 * Creates a manager that, when enabled, instruments with the given configuration (e.g. a specific
	 * {@link io.opentelemetry.api.OpenTelemetry} instance rather than the global default).
	 */
	public TracingRemoteRepositoryManager(String serverURL, RDF4JOpenTelemetryConfig config) {
		super(serverURL);
		this.config = Objects.requireNonNull(config, "config must not be null");
	}

	@Override
	protected Repository createRepository(String id) throws RepositoryConfigException, RepositoryException {
		Repository repository = super.createRepository(id);
		if (repository == null || !OpenTelemetrySettings.isEnabled()) {
			return repository;
		}
		return OpenTelemetrySupport.instrument(repository, id, config);
	}
}
