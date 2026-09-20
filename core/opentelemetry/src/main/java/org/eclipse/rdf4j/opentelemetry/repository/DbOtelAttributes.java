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

import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;

import io.opentelemetry.api.common.AttributeKey;

/**
 * Span attribute keys used by this package's {@link org.eclipse.rdf4j.repository.RepositoryConnection}-level tracing
 * instrumentation, following the
 * <a href="https://opentelemetry.io/docs/specs/semconv/database/database-spans/">OpenTelemetry semantic conventions for
 * database client calls</a>.
 */
public final class DbOtelAttributes {

	private DbOtelAttributes() {
	}

	/** The database system, e.g. {@code "rdf4j"}. */
	public static final AttributeKey<String> DB_SYSTEM_NAME = AttributeKey.stringKey("db.system.name");

	/** The kind of operation: {@code SELECT}, {@code CONSTRUCT}, {@code DESCRIBE}, {@code ASK}, or {@code UPDATE}. */
	public static final AttributeKey<String> DB_OPERATION_NAME = AttributeKey.stringKey("db.operation.name");

	/** The repository identifier, as supplied to {@code OpenTelemetrySupport.instrument(Repository, String)}. */
	public static final AttributeKey<String> DB_NAMESPACE = AttributeKey.stringKey("db.namespace");

	/**
	 * The (possibly truncated) SPARQL query or update text. Only recorded when explicitly enabled via
	 * {@link RDF4JOpenTelemetryConfig.Builder#captureQueryText(boolean)}.
	 */
	public static final AttributeKey<String> DB_QUERY_TEXT = AttributeKey.stringKey("db.query.text");

	/** A low-cardinality summary of the operation; equal to the span name. */
	public static final AttributeKey<String> DB_QUERY_SUMMARY = AttributeKey.stringKey("db.query.summary");

	/** The number of rows/statements returned by a tuple or graph query, set when the result is closed. */
	public static final AttributeKey<Long> DB_RESPONSE_RETURNED_ROWS = AttributeKey
			.longKey("db.response.returned_rows");

	/** The fully-qualified class name of the exception, set on failure. */
	public static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");
}
