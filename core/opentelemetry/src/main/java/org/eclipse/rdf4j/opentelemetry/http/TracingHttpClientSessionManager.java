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
package org.eclipse.rdf4j.opentelemetry.http;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.opentelemetry.OpenTelemetrySupport;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;

/**
 * A {@link HttpClientSessionManager} that creates {@link TracingSPARQLProtocolSession}/
 * {@link TracingRDF4JProtocolSession} instances instead of plain sessions, so that every outbound HTTP request made
 * through it is recorded as an OpenTelemetry span.
 * <p>
 * Extends {@link SharedHttpClientSessionManager} to reuse its HTTP client and executor lifecycle management; only
 * session creation is overridden.
 *
 * @see OpenTelemetrySupport
 */
public class TracingHttpClientSessionManager extends SharedHttpClientSessionManager {

	private final RDF4JOpenTelemetryConfig config;

	private final Map<AutoCloseable, Boolean> openTracingSessions = new ConcurrentHashMap<>();

	/**
	 * Creates a session manager using {@link RDF4JOpenTelemetryConfig#defaultConfig()}, managing its own HTTP client
	 * and executor.
	 */
	public TracingHttpClientSessionManager() {
		this(RDF4JOpenTelemetryConfig.defaultConfig());
	}

	/**
	 * Creates a session manager with the given OpenTelemetry configuration, managing its own HTTP client and executor.
	 *
	 * @param config the OpenTelemetry configuration to use for all sessions created by this manager
	 */
	public TracingHttpClientSessionManager(RDF4JOpenTelemetryConfig config) {
		this.config = Objects.requireNonNull(config, "config must not be null");
	}

	/**
	 * Creates a session manager with the given OpenTelemetry configuration, using an externally managed HTTP client and
	 * executor (whose lifecycle this manager does not own/close).
	 *
	 * @param dependentClient          the HTTP client to use
	 * @param dependentExecutorService the executor to use
	 * @param config                   the OpenTelemetry configuration to use for all sessions created by this manager
	 */
	public TracingHttpClientSessionManager(RDF4JHttpClient dependentClient, ExecutorService dependentExecutorService,
			RDF4JOpenTelemetryConfig config) {
		super(dependentClient, dependentExecutorService);
		this.config = Objects.requireNonNull(config, "config must not be null");
	}

	@Override
	public SPARQLProtocolSession createSPARQLProtocolSession(String queryEndpointUrl, String updateEndpointUrl) {
		TracingSPARQLProtocolSession session = new TracingSPARQLProtocolSession(getHttpClient(), getExecutorService(),
				config, queryEndpointUrl, updateEndpointUrl) {

			@Override
			public void close() {
				try {
					super.close();
				} finally {
					openTracingSessions.remove(this);
				}
			}
		};
		openTracingSessions.put(session, true);
		return session;
	}

	@Override
	public RDF4JProtocolSession createRDF4JProtocolSession(String serverURL) {
		TracingRDF4JProtocolSession session = new TracingRDF4JProtocolSession(getHttpClient(), getExecutorService(),
				config, serverURL) {

			@Override
			public void close() {
				try {
					super.close();
				} finally {
					openTracingSessions.remove(this);
				}
			}
		};
		openTracingSessions.put(session, true);
		return session;
	}

	@Override
	public void shutDown() {
		try {
			openTracingSessions.keySet().forEach(session -> {
				try {
					session.close();
				} catch (Exception e) {
					// best effort; the executor/HTTP client shutdown below still releases the underlying resources
				}
			});
		} finally {
			super.shutDown();
		}
	}
}
