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

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

/**
 * Wraps a {@link RepositoryResult} so that the span covering a {@code getStatements(...)} call stays open for the full
 * lifetime of result iteration (not just the, often lazy, initial call), counts rows, and records exceptions raised
 * while iterating.
 */
class TracingRepositoryResult<T> extends RepositoryResult<T> {

	private final Span span;
	private long rowCount = 0;

	TracingRepositoryResult(RepositoryResult<T> delegate, Span span) {
		super(delegate);
		this.span = span;
	}

	@Override
	public boolean hasNext() throws RepositoryException {
		try (Scope scope = span.makeCurrent()) {
			boolean hasNext = super.hasNext();
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
	public T next() throws RepositoryException {
		try (Scope scope = span.makeCurrent()) {
			T value = super.next();
			rowCount++;
			return value;
		} catch (RuntimeException e) {
			TracingOperation.recordException(e, span);
			throw e;
		}
	}

	@Override
	protected void handleClose() throws RepositoryException {
		try {
			super.handleClose();
		} finally {
			span.setAttribute(DbOtelAttributes.DB_RESPONSE_RETURNED_ROWS, rowCount);
			span.end();
		}
	}
}
