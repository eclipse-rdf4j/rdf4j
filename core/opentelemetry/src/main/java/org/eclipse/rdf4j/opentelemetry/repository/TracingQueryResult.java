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

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResult;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

/**
 * Wraps a {@link QueryResult} so that the span covering a query stays open for the full lifetime of result iteration
 * (not just the, often lazy, {@code evaluate()} call), counts rows, and records exceptions raised while iterating.
 */
class TracingQueryResult<T> implements QueryResult<T> {

	private final QueryResult<T> delegate;
	private final Span span;
	private long rowCount = 0;
	private boolean closed = false;

	TracingQueryResult(QueryResult<T> delegate, Span span) {
		this.delegate = delegate;
		this.span = span;
	}

	protected QueryResult<T> getDelegate() {
		return delegate;
	}

	@Override
	public void close() {
		if (!closed) {
			try {
				delegate.close();
			} finally {
				span.setAttribute(DbOtelAttributes.DB_RESPONSE_RETURNED_ROWS, rowCount);
				span.end();
				closed = true;
			}
		}
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		try (Scope scope = span.makeCurrent()) {
			boolean hasNext = delegate.hasNext();
			if (!hasNext) {
				// callers that iterate to exhaustion without an explicit close() must still get the span ended
				// and the row count recorded
				close();
			}
			return hasNext;
		} catch (RuntimeException e) {
			TracingOperation.recordException(e, span);
			throw e;
		}
	}

	@Override
	public T next() throws QueryEvaluationException {
		try (Scope scope = span.makeCurrent()) {
			T value = delegate.next();
			rowCount++;
			return value;
		} catch (RuntimeException e) {
			TracingOperation.recordException(e, span);
			throw e;
		}
	}
}
