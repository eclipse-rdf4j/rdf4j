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
import java.net.URI;

import org.eclipse.rdf4j.http.client.spi.HttpRequest;
import org.eclipse.rdf4j.http.client.spi.HttpResponse;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;

/**
 * Shared span-lifecycle logic used by both {@link TracingSPARQLProtocolSession} and
 * {@link TracingRDF4JProtocolSession}. Not public API: both session classes hold their own instance and delegate to it
 * from their {@code getQueryMethod}/{@code getUpdateMethod}/{@code execute} overrides, since Java's single inheritance
 * means the two session classes can't share a common tracing superclass.
 * <p>
 * Instances are only safe to use the way {@code SPARQLProtocolSession} itself is documented to be used: one
 * request/response cycle at a time on a given session.
 */
final class TracingSessionSupport {

	@FunctionalInterface
	interface RequestExecution {
		HttpResponse execute() throws IOException;
	}

	private final Tracer tracer;
	private final TextMapPropagator propagator;
	private final boolean captureQueryText;
	private final int maxQueryTextLength;

	private volatile PendingOperation pending;

	TracingSessionSupport(RDF4JOpenTelemetryConfig config) {
		this.tracer = config.getTracer();
		this.propagator = config.getOpenTelemetry().getPropagators().getTextMapPropagator();
		this.captureQueryText = config.isCaptureQueryText();
		this.maxQueryTextLength = config.getMaxQueryTextLength();
	}

	/**
	 * Records the SPARQL operation about to be turned into an {@link HttpRequest}, so that the next call to
	 * {@link #trace(HttpRequest, RequestExecution)} on this session can attach it as span attributes.
	 */
	void capture(String operation, String text) {
		this.pending = new PendingOperation(operation, text);
	}

	/**
	 * Wraps a single outbound HTTP request/response cycle in a CLIENT span: injects trace-context propagation headers,
	 * attaches HTTP and (if captured) SPARQL attributes, and records the outcome.
	 */
	HttpResponse trace(HttpRequest request, RequestExecution execution) throws IOException {
		PendingOperation operation = this.pending;
		this.pending = null;

		String spanName = operation != null
				? SparqlOtelAttributes.SPAN_NAME_PREFIX + operation.operation
				: "HTTP " + request.getMethod();

		Span span = tracer.spanBuilder(spanName)
				.setSpanKind(SpanKind.CLIENT)
				.startSpan();

		try {
			span.setAttribute(SparqlOtelAttributes.HTTP_REQUEST_METHOD, request.getMethod());
			span.setAttribute(SparqlOtelAttributes.URL_FULL, request.getUri().toString());

			URI uri = request.getUri();
			if (uri.getHost() != null) {
				span.setAttribute(SparqlOtelAttributes.SERVER_ADDRESS, uri.getHost());
			}
			if (uri.getPort() != -1) {
				span.setAttribute(SparqlOtelAttributes.SERVER_PORT, (long) uri.getPort());
			}
			if (operation != null) {
				span.setAttribute(SparqlOtelAttributes.SPARQL_OPERATION, operation.operation);
				if (captureQueryText && operation.text != null) {
					span.setAttribute(SparqlOtelAttributes.SPARQL_QUERY, truncate(operation.text));
				}
			}

			try (Scope scope = span.makeCurrent()) {
				propagator.inject(Context.current(), request, RequestHeaderSetter.INSTANCE);
				HttpResponse response = execution.execute();
				span.setAttribute(SparqlOtelAttributes.HTTP_RESPONSE_STATUS_CODE, (long) response.getStatusCode());
				if (response.getStatusCode() >= 400) {
					span.setStatus(StatusCode.ERROR);
				}
				return response;
			} catch (Throwable t) {
				span.recordException(t);
				span.setStatus(StatusCode.ERROR, t.getMessage() != null ? t.getMessage() : t.getClass().getName());
				throw t;
			}
		} finally {
			span.end();
		}
	}

	private String truncate(String text) {
		if (text.length() > maxQueryTextLength) {
			return text.substring(0, maxQueryTextLength);
		}
		return text;
	}

	private static final class PendingOperation {
		final String operation;
		final String text;

		PendingOperation(String operation, String text) {
			this.operation = operation;
			this.text = text;
		}
	}
}
