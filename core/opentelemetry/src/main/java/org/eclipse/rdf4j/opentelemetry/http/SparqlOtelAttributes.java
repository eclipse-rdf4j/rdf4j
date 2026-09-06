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

import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;

import io.opentelemetry.api.common.AttributeKey;

/**
 * Span attribute keys used by this module's tracing instrumentation.
 * <p>
 * The generic HTTP attributes follow the
 * <a href="https://opentelemetry.io/docs/specs/semconv/http/http-spans/">OpenTelemetry semantic conventions for HTTP
 * clients</a>. The {@code rdf4j.sparql.*} attributes are RDF4J-specific, since there is currently no OpenTelemetry
 * semantic convention covering the SPARQL protocol.
 */
public final class SparqlOtelAttributes {

	private SparqlOtelAttributes() {
	}

	/** The HTTP request method, e.g. {@code GET} or {@code POST}. */
	public static final AttributeKey<String> HTTP_REQUEST_METHOD = AttributeKey.stringKey("http.request.method");

	/** The full request URL. */
	public static final AttributeKey<String> URL_FULL = AttributeKey.stringKey("url.full");

	/** The host of the remote endpoint. */
	public static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");

	/** The port of the remote endpoint. */
	public static final AttributeKey<Long> SERVER_PORT = AttributeKey.longKey("server.port");

	/** The HTTP response status code. */
	public static final AttributeKey<Long> HTTP_RESPONSE_STATUS_CODE = AttributeKey
			.longKey("http.response.status_code");

	/** Either {@code "query"} or {@code "update"}, identifying the kind of SPARQL operation sent. */
	public static final AttributeKey<String> SPARQL_OPERATION = AttributeKey.stringKey("rdf4j.sparql.operation");

	/**
	 * The (possibly truncated) SPARQL query or update text. Only recorded when explicitly enabled via
	 * {@link RDF4JOpenTelemetryConfig.Builder#captureQueryText(boolean)}.
	 */
	public static final AttributeKey<String> SPARQL_QUERY = AttributeKey.stringKey("rdf4j.sparql.query");

	static final String SPAN_NAME_PREFIX = "SPARQL ";
}
