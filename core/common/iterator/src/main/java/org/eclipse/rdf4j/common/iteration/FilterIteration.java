/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A CloseableIteration that wraps another Iteration, applying a filter on the objects that are returned. Subclasses
 * must implement the <var>accept</var> method to indicate which objects should be returned.
 */
public abstract class FilterIteration<K extends CloseableIteration<? extends E, X>, E, X extends Exception>
		extends AbstractCloseableIteration<E, X> {

	protected final K wrappedIter;

	/*-----------*
	 * Variables *
	 *-----------*/

	private E nextElement;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * @param iter
	 */
	protected FilterIteration(K iter) {
		wrappedIter = Objects.requireNonNull(iter);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean hasNext() throws X {
		if (isClosed()) {
			return false;
		} else if (Thread.currentThread().isInterrupted()) {
			close();
			return false;
		}

		findNextElement();

		boolean result = nextElement != null;
		if (!result) {
			close();
		}
		return result;
	}

	@Override
	public E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		findNextElement();

		E result = nextElement;

		if (result != null) {
			nextElement = null;
			return result;
		} else {
			close();
			throw new NoSuchElementException("The iteration has been closed.");
		}
	}

	private void findNextElement() throws X {
		try {
			while (!isClosed() && nextElement == null && wrappedIter.hasNext()) {
				E candidate = wrappedIter.next();

				if (accept(candidate)) {
					nextElement = candidate;
				}
			}
		} finally {
			if (isClosed()) {
				nextElement = null;
			}
		}
	}

	/**
	 * Tests whether or not the specified object should be returned by this Iteration. All objects from the wrapped
	 * Iteration pass through this method in the same order as they are coming from the wrapped Iteration.
	 *
	 * @param object The object to be tested.
	 * @return <var>true</var> if the object should be returned, <var>false</var> otherwise.
	 * @throws X
	 */
	protected abstract boolean accept(E object) throws X;

	/**
	 * Removes the last element that has been returned from the wrapped Iteration.
	 *
	 * @throws UnsupportedOperationException If the wrapped Iteration does not support the <var>remove</var> operation.
	 * @throws IllegalStateException         if the Iteration has been closed, or if {@link #next} has not yet been
	 *                                       called, or {@link #remove} has already been called after the last call to
	 *                                       {@link #next}.
	 */
	@Override
	public final void remove() throws X {
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		} else if (Thread.currentThread().isInterrupted()) {
			close();
			throw new IllegalStateException("The iteration has been interrupted.");
		}
		try {
			wrappedIter.remove();
		} catch (IllegalStateException e) {
			close();
			throw e;
		}
	}

	/**
	 * Closes this Iteration and also closes the wrapped Iteration if it is a {@link CloseableIteration}.
	 */
	@Override
	protected void handleClose() throws X {
		wrappedIter.close();
	}
}
