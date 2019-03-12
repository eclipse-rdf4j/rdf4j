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
 * A CloseableIteration that converts an iteration over objects of type <tt>S</tt> (the source type) to an iteration
 * over objects of type <tt>T</tt> (the target type).
 */
public abstract class ConvertingIteration<S, T, X extends Exception> extends AbstractCloseableIteration<T, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The source type iteration.
	 */
	private final Iteration<? extends S, ? extends X> iter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new ConvertingIteration that operates on the supplied source type iteration.
	 * 
	 * @param iter The source type iteration for this <tt>ConvertingIteration</tt>, must not be <tt>null</tt>.
	 */
	public ConvertingIteration(Iteration<? extends S, ? extends X> iter) {
		this.iter = Objects.requireNonNull(iter, "The iterator was null");
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Converts a source type object to a target type object.
	 */
	protected abstract T convert(S sourceObject) throws X;

	/**
	 * Checks whether the source type iteration contains more elements.
	 * 
	 * @return <tt>true</tt> if the source type iteration contains more elements, <tt>false</tt> otherwise.
	 * @throws X
	 */
	@Override
	public boolean hasNext() throws X {
		if (isClosed()) {
			return false;
		}
		boolean result = iter.hasNext();
		if (!result) {
			close();
		}
		return result;
	}

	/**
	 * Returns the next element from the source type iteration.
	 * 
	 * @throws X
	 * @throws                       java.util.NoSuchElementException If all elements have been returned.
	 * @throws IllegalStateException If the iteration has been closed.
	 */
	@Override
	public T next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		return convert(iter.next());
	}

	/**
	 * Calls <tt>remove()</tt> on the underlying Iteration.
	 * 
	 * @throws UnsupportedOperationException If the wrapped Iteration does not support the <tt>remove</tt> operation.
	 * @throws IllegalStateException         If the Iteration has been closed, or if {@link #next} has not yet been
	 *                                       called, or {@link #remove} has already been called after the last call to
	 *                                       {@link #next}.
	 */
	@Override
	public void remove() throws X {
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		iter.remove();
	}

	/**
	 * Closes this iteration as well as the wrapped iteration if it is a {@link CloseableIteration}.
	 */
	@Override
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			Iterations.closeCloseable(iter);
		}
	}
}
