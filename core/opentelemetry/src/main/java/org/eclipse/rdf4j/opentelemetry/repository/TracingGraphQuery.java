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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/** Traces a {@link GraphQuery} evaluation as a {@code CONSTRUCT}/{@code DESCRIBE} span. */
class TracingGraphQuery extends TracingQuery<GraphQuery> implements GraphQuery {

	// Matches the query form keyword once any leading PREFIX/BASE prolog has been stripped.
	private static final Pattern LEADING_KEYWORD = Pattern.compile("(?i)^\\s*(\\w+)");

	private final String operationName;

	TracingGraphQuery(GraphQuery delegate, String operationString, String repositoryId,
			RDF4JOpenTelemetryConfig config, Tracer tracer) {
		super(delegate, operationString, repositoryId, config, tracer);
		this.operationName = detectOperationName(operationString);
	}

	// RDF4J represents CONSTRUCT and DESCRIBE with the same GraphQuery type, so the query form is not available via
	// the API and is derived from the query text instead. Tracing must never break query execution, so any
	// detection failure falls back to CONSTRUCT.
	private static String detectOperationName(String operationString) {
		if (operationString == null) {
			return "CONSTRUCT";
		}
		try {
			String withoutProlog = QueryParserUtil.removeSPARQLQueryProlog(operationString);
			Matcher m = LEADING_KEYWORD.matcher(withoutProlog);
			if (m.find() && "DESCRIBE".equalsIgnoreCase(m.group(1))) {
				return "DESCRIBE";
			}
		} catch (RuntimeException e) {
			// fall through to default
		}
		return "CONSTRUCT";
	}

	@Override
	public GraphQueryResult evaluate() throws QueryEvaluationException {
		Span span = startSpan();
		try (Scope scope = span.makeCurrent()) {
			return new TracingGraphQueryResult(getDelegate().evaluate(), span);
		} catch (RuntimeException e) {
			recordException(e, span);
			span.end();
			throw e;
		}
	}

	@Override
	public void evaluate(RDFHandler handler) throws QueryEvaluationException, RDFHandlerException {
		Span span = startSpan();
		try (Scope scope = span.makeCurrent()) {
			getDelegate().evaluate(handler);
		} catch (RuntimeException e) {
			recordException(e, span);
			throw e;
		} finally {
			span.end();
		}
	}

	@Override
	protected String getOperationName() {
		return operationName;
	}
}
