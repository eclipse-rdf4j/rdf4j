/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;

/**
 * An iteration that delays the creation of the underlying iteration until it is being accessed. This is mainly useful
 * for situations where iteration creation adds considerable overhead but where the iteration may not actually be used,
 * or where a created iteration consumes scarce resources like JDBC-connections or memory. Subclasses must implement the
 * <var>createIteration</var> method, which is called once when the iteration is first needed.
 */
@Deprecated(since = "4.1.0")
public abstract class DelayedIteration<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Iteration<? extends E, ? extends X> iter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new DelayedIteration.
	 */
	protected DelayedIteration() {
		super();
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Creates the iteration that should be iterated over. This method is called only once, when the iteration is first
	 * needed.
	 */
	protected abstract Iteration<? extends E, ? extends X> createIteration() throws X;

	/**
	 * Calls the <var>hasNext</var> method of the underlying iteration.
	 */
	@Override
	public boolean hasNext() throws X {
		if (isClosed()) {
			return false;
		}
		Iteration<? extends E, ? extends X> resultIter = iter;
		if (resultIter == null) {
			// Underlying iterator has not yet been initialized
			resultIter = iter;
			if (resultIter == null) {
				resultIter = iter = createIteration();
			}
		}

		return resultIter.hasNext();
	}

	/**
	 * Calls the <var>next</var> method of the underlying iteration.
	 */
	@Override
	public E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("Iteration has been closed");
		}
		Iteration<? extends E, ? extends X> resultIter = iter;
		if (resultIter == null) {
			// Underlying iterator has not yet been initialized
			resultIter = iter;
			if (resultIter == null) {
				resultIter = iter = createIteration();
			}
		}

		return resultIter.next();
	}

	/**
	 * Calls the <var>remove</var> method of the underlying iteration.
	 */
	@Override
	public void remove() throws X {
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		Iteration<? extends E, ? extends X> resultIter = iter;
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
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			Iteration<? extends E, ? extends X> toClose = iter;
			if (toClose != null) {
				Iterations.closeCloseable(toClose);
			}
		}
	}
}
