/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.base;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;

public class CloseableQueryEvalutationExceptionConvertingIteration<E, X extends Exception, T extends CloseableIteration<? extends E, ? extends Exception>>
		implements CloseableIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The underlying Iteration.
	 */
	private final T iter;
	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private boolean closed = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new CloseableExceptionConvertingIteration that operates on the supplied iteration.
	 *
	 * @param iter The Iteration that this <var>CloseableExceptionConvertingIteration</var> operates on, must not be
	 *             <var>null</var>.
	 */
	public CloseableQueryEvalutationExceptionConvertingIteration(T iter) {
		this.iter = Objects.requireNonNull(iter, "The iterator was null");
	}

	/**
	 * Checks whether the underlying Iteration contains more elements.
	 *
	 * @return <var>true</var> if the underlying Iteration contains more elements, <var>false</var> otherwise.
	 * @throws X
	 */
	@Override
	public final boolean hasNext() throws X {
		if (isClosed()) {
			return false;
		}
		try {
			boolean result = iter.hasNext();
			if (!result) {
				close();
			}
			return result;
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}
	}

	/**
	 * Returns the next element from the wrapped Iteration.
	 *
	 * @throws X
	 * @throws NoSuchElementException If all elements have been returned.
	 * @throws IllegalStateException  If the Iteration has been closed.
	 */
	@Override
	public final E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		try {
			return iter.next();
		} catch (NoSuchElementException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}
	}

	/**
	 * Calls <var>remove()</var> on the underlying Iteration.
	 *
	 * @throws UnsupportedOperationException If the wrapped Iteration does not support the <var>remove</var> operation.
	 * @throws IllegalStateException         If the Iteration has been closed, or if {@link #next} has not yet been
	 *                                       called, or {@link #remove} has already been called after the last call to
	 *                                       {@link #next}.
	 */
	@Override
	public final void remove() throws X {
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		try {
			iter.remove();
		} catch (UnsupportedOperationException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}
	}

	/**
	 * Checks whether this CloseableIteration has been closed.
	 *
	 * @return <var>true</var> if the CloseableIteration has been closed, <var>false</var> otherwise.
	 */
	@Override
	public final boolean isClosed() {
		return closed;
	}

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure the resource closures are only executed once.
	 */
	@Override
	public final void close() throws X {
		if (!closed) {
			closed = true;
			try {
				iter.close();
			} catch (Exception e) {
				throw new QueryEvaluationException(e);
			}
		}
	}
}
