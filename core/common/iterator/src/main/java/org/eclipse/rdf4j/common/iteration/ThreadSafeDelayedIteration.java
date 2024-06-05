/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;

/**
 * An iteration that delays the creation of the underlying iteration until it is being accessed. This is mainly useful
 * for situations where iteration creation adds considerable overhead but where the iteration may not actually be used,
 * or where a created iteration consumes scarce resources like JDBC-connections or memory. Subclasses must implement the
 * <var>createIteration</var> method, which is called once when the iteration is first needed.
 */
public abstract class ThreadSafeDelayedIteration<E> extends AbstractCloseableIteration<E> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile CloseableIteration<? extends E> iter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new DelayedIteration.
	 */
	protected ThreadSafeDelayedIteration() {
		super();
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Creates the iteration that should be iterated over. This method is called only once, when the iteration is first
	 * needed.
	 */
	protected abstract CloseableIteration<? extends E> createIteration();

	/**
	 * Calls the <var>hasNext</var> method of the underlying iteration.
	 */
	@Override
	public boolean hasNext() {
		if (isClosed()) {
			return false;
		}
		CloseableIteration<? extends E> resultIter = iter;
		if (resultIter == null) {
			synchronized (this) {
				resultIter = iter;
				if (resultIter == null) {
					// Underlying iterator has not yet been initialized
					resultIter = iter = createIteration();
				}
			}
		}

		return resultIter.hasNext();
	}

	/**
	 * Calls the <var>next</var> method of the underlying iteration.
	 */
	@Override
	public E next() {
		if (isClosed()) {
			throw new NoSuchElementException("Iteration has been closed");
		}
		CloseableIteration<? extends E> resultIter = iter;
		if (resultIter == null) {
			synchronized (this) {
				resultIter = iter;
				if (resultIter == null) {
					// Underlying iterator has not yet been initialized
					resultIter = iter = createIteration();
				}
			}
		}

		return resultIter.next();
	}

	/**
	 * Calls the <var>remove</var> method of the underlying iteration.
	 */
	@Override
	public void remove() {
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		CloseableIteration<? extends E> resultIter = iter;
		if (resultIter == null) {
			throw new IllegalStateException("Underlying iteration was null");
		}

		resultIter.remove();
	}

	/**
	 * Closes this iteration as well as the underlying iteration if it has already been created and happens to be a
	 * {@link CloseableIteration}.
	 */
	@Override
	protected void handleClose() {
		if (iter != null) {
			iter.close();
		}
	}
}
