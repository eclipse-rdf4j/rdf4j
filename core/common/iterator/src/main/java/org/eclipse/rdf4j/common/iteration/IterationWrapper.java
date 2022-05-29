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
 * Abstract superclass for Iterations that wrap other Iterations. The abstract class <var>IterationWrapper</var> itself
 * provides default methods that forward method calls to the wrapped Iteration. Subclasses of
 * <var>IterationWrapper</var> should override some of these methods and may also provide additional methods and fields.
 */
@Deprecated(since = "4.1.0")
public class IterationWrapper<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The wrapped Iteration.
	 *
	 * @deprecated This will be changed to private, possibly with an accessor in future. Do not rely on it.
	 */
	@Deprecated
	protected final Iteration<? extends E, ? extends X> wrappedIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new IterationWrapper that operates on the supplied Iteration.
	 *
	 * @param iter The wrapped Iteration for this <var>IterationWrapper</var>, must not be <var>null</var>.
	 */
	protected IterationWrapper(Iteration<? extends E, ? extends X> iter) {
		assert iter != null;
		wrappedIter = iter;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Checks whether the wrapped Iteration contains more elements, closing this Iteration when this is not the case.
	 *
	 * @return <var>true</var> if the wrapped Iteration contains more elements, <var>false</var> otherwise.
	 */
	@Override
	public boolean hasNext() throws X {
		if (isClosed()) {
			return false;
		} else if (Thread.currentThread().isInterrupted()) {
			close();
			return false;
		}
		boolean result = wrappedIter.hasNext();
		if (!result) {
			close();
		}
		return result;
	}

	/**
	 * Returns the next element from the wrapped Iteration.
	 *
	 * @throws java.util.NoSuchElementException If all elements have been returned or it has been closed.
	 */
	@Override
	public E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		} else if (Thread.currentThread().isInterrupted()) {
			close();
			throw new NoSuchElementException("The iteration has been interrupted.");
		}
		try {
			return wrappedIter.next();
		} catch (NoSuchElementException e) {
			close();
			throw e;
		}
	}

	/**
	 * Removes the last element that has been returned from the wrapped Iteration.
	 *
	 * @throws UnsupportedOperationException If the wrapped Iteration does not support the <var>remove</var> operation.
	 * @throws IllegalStateException         if the Iteration has been closed, or if {@link #next} has not yet been
	 *                                       called, or {@link #remove} has already been called after the last call to
	 *                                       {@link #next}.
	 */
	@Override
	public void remove() throws X {
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
		try {
			super.handleClose();
		} finally {
			Iterations.closeCloseable(wrappedIter);
		}
	}
}
