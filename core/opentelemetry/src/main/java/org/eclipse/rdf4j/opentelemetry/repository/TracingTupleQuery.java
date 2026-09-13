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
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/** Traces a {@link TupleQuery} evaluation as a {@code db.operation.name = SELECT} span. */
class TracingTupleQuery extends TracingQuery<TupleQuery> implements TupleQuery {

	TracingTupleQuery(TupleQuery delegate, String operationString, String repositoryId,
			RDF4JOpenTelemetryConfig config, Tracer tracer) {
		super(delegate, operationString, repositoryId, config, tracer);
	}

	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		Span span = startSpan();
		try (Scope scope = span.makeCurrent()) {
			return new TracingTupleQueryResult(getDelegate().evaluate(), span);
		} catch (RuntimeException e) {
			recordException(e, span);
			span.end();
			throw e;
		}
	}

	@Override
	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {
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
		return "SELECT";
	}
}
