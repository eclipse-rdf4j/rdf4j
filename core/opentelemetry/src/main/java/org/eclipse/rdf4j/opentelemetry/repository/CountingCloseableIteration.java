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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;

/**
 * Wraps a {@link CloseableIteration} to count the elements pulled through it via {@link #getCount()}, without otherwise
 * altering its behaviour. Used to determine the number of statements written/removed by a bulk
 * {@code add}/{@code remove} call, without requiring a separate pass over the input.
 */
class CountingCloseableIteration<T> implements CloseableIteration<T> {

	private final CloseableIteration<T> delegate;
	private long count = 0;

	CountingCloseableIteration(CloseableIteration<T> delegate) {
		this.delegate = delegate;
	}

	long getCount() {
		return count;
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public T next() {
		T next = delegate.next();
		count++;
		return next;
	}

	@Override
	public void remove() {
		delegate.remove();
	}

	@Override
	public void close() {
		delegate.close();
	}
}
