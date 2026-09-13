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

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.http.client.spi.HttpRequest;
import org.eclipse.rdf4j.http.client.spi.HttpResponse;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryLanguage;

/**
 * A {@link SPARQLProtocolSession} that records an OpenTelemetry CLIENT span for every outbound HTTP request, propagates
 * the current trace context (W3C {@code traceparent}/{@code tracestate}) into the request, and optionally records the
 * SPARQL query/update text.
 * <p>
 * Instances are usually obtained via {@link TracingHttpClientSessionManager}; this constructor is available for
 * advanced/custom wiring.
 */
public class TracingSPARQLProtocolSession extends SPARQLProtocolSession {

	private final TracingSessionSupport tracing;

	/**
	 * @param client    the HTTP client to use
	 * @param executor  the executor used for background result parsing
	 * @param config    the OpenTelemetry configuration to use
	 * @param queryURL  the SPARQL query endpoint URL
	 * @param updateURL the SPARQL update endpoint URL
	 */
	public TracingSPARQLProtocolSession(RDF4JHttpClient client, ExecutorService executor,
			RDF4JOpenTelemetryConfig config, String queryURL, String updateURL) {
		super(client, executor);
		setQueryURL(queryURL);
		setUpdateURL(updateURL);
		this.tracing = new TracingSessionSupport(config);
	}

	@Override
	protected HttpRequest getQueryMethod(QueryLanguage ql, String query, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings) {
		tracing.capture("query", query);
		return super.getQueryMethod(ql, query, baseURI, dataset, includeInferred, maxQueryTime, bindings);
	}

	@Override
	protected HttpRequest getUpdateMethod(QueryLanguage ql, String update, String baseURI, Dataset dataset,
			boolean includeInferred, int maxQueryTime, Binding... bindings) {
		tracing.capture("update", update);
		return super.getUpdateMethod(ql, update, baseURI, dataset, includeInferred, maxQueryTime, bindings);
	}

	@Override
	protected HttpResponse execute(HttpRequest request) throws IOException, RDF4JException {
		return tracing.trace(request, () -> super.execute(request));
	}
}
