/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;

/**
 * A CloseableIteration that wraps another Iteration, applying a filter on the objects that are returned. Subclasses
 * must implement the <var>accept</var> method to indicate which objects should be returned.
 */
public abstract class FilterIteration<E> implements CloseableIteration<E> {
	/**
	 * The wrapped Iteration.
	 *
	 * @deprecated This will be changed to private, possibly with an accessor in future. Do not rely on it.
	 */
	private final CloseableIteration<? extends E> wrappedIter;

	/*-----------*
	 * Variables *
	 *-----------*/

	private E nextElement;
	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private boolean closed = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * @param iter
	 */
	protected FilterIteration(CloseableIteration<? extends E> iter) {
		assert iter != null;
		this.wrappedIter = iter;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean hasNext() {
		if (isClosed()) {
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
	public E next() {
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

	private void findNextElement() {
		if (nextElement != null) {
			return;
		}

		try {
			if (!isClosed()) {
				if (Thread.currentThread().isInterrupted()) {
					close();
					return;
				} else {
					boolean result = wrappedIter.hasNext();
					if (!result) {
						close();
						return;
					}
				}
			}

			// We know that nextElement has to be null and that wrappedIter.hasNext() must be true, based on the code
			// above. To be sure that these invariants don't change we also assert them below.
			assert nextElement == null && wrappedIter.hasNext();

			do {
				E result;
				if (Thread.currentThread().isInterrupted()) {
					close();
					return;
				}
				try {
					result = wrappedIter.next();
				} catch (NoSuchElementException e) {
					close();
					throw e;
				}
				E candidate = result;

				if (accept(candidate)) {
					nextElement = candidate;
				}
			} while (nextElement == null && wrappedIter.hasNext());

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
	 */
	protected abstract boolean accept(E object);

	/**
	 * Removes the last element that has been returned from the wrapped Iteration.
	 *
	 * @throws UnsupportedOperationException If the wrapped Iteration does not support the <var>remove</var> operation.
	 * @throws IllegalStateException         if the Iteration has been closed, or if {@link #next} has not yet been
	 *                                       called, or {@link #remove} has already been called after the last call to
	 *                                       {@link #next}.
	 */
	@Override
	public void remove() {
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

	private boolean isClosed() {
		return closed;
	}

	/**
	 * Closes this Iteration and also closes the wrapped Iteration if it is a {@link CloseableIteration}.
	 */
	abstract protected void handleClose();

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure the resource closures are only executed once.
	 */
	@Override
	public final void close() {
		if (!closed) {
			closed = true;
			try {
				wrappedIter.close();
			} finally {
				handleClose();
			}
		}
	}
}
