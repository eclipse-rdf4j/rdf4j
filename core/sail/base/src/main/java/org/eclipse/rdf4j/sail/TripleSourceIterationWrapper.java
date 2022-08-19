/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail;

import java.util.NoSuchElementException;
import java.util.Objects;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;

@InternalUseOnly
public class TripleSourceIterationWrapper<T> implements CloseableIteration<T, QueryEvaluationException> {

	private final CloseableIteration<? extends T, SailException> delegate;
	private boolean closed = false;

	public TripleSourceIterationWrapper(CloseableIteration<? extends T, SailException> delegate) {
		this.delegate = Objects.requireNonNull(delegate, "The iterator was null");
	}

	/**
	 * Checks whether the underlying iteration contains more elements.
	 *
	 * @return <var>true</var> if the underlying iteration contains more elements, <var>false</var> otherwise.
	 * @throws QueryEvaluationException
	 */
	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (closed) {
			return false;
		}
		try {
			boolean result = delegate.hasNext();
			if (!result) {
				close();
			}
			return result;
		} catch (QueryEvaluationException e) {
			throw e;
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new QueryEvaluationException(e);
		}
	}

	/**
	 * Returns the next element from the wrapped iteration.
	 *
	 * @throws QueryEvaluationException
	 * @throws NoSuchElementException   If all elements have been returned.
	 * @throws IllegalStateException    If the iteration has been closed.
	 */
	@Override
	public T next() throws QueryEvaluationException {
		if (closed) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		try {
			return delegate.next();
		} catch (NoSuchElementException e) {
			close();
			throw e;
		} catch (IllegalStateException | QueryEvaluationException e) {
			throw e;
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new QueryEvaluationException(e);
		}
	}

	/**
	 * Calls <var>remove()</var> on the underlying iteration.
	 *
	 * @throws UnsupportedOperationException If the wrapped iteration does not support the <var>remove</var> operation.
	 * @throws IllegalStateException         If the Iteration has been closed, or if {@link #next} has not yet been
	 *                                       called, or {@link #remove} has already been called after the last call to
	 *                                       {@link #next}.
	 */
	@Override
	public void remove() throws QueryEvaluationException {
		if (closed) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		try {
			delegate.remove();
		} catch (UnsupportedOperationException | IllegalStateException | QueryEvaluationException e) {
			throw e;
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new QueryEvaluationException(e);
		}
	}

	@Override
	public final void close() throws QueryEvaluationException {
		if (!closed) {
			closed = true;
			delegate.close();
		}
	}
}
