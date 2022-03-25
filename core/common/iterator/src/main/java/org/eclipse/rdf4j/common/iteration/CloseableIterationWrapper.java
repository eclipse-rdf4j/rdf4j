/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class CloseableIterationWrapper<T extends CloseableIteration<? extends E, ? extends X>, E, X extends Exception>
		extends AbstractCloseableIteration<E, X> {

	protected final T wrappedIter;

	/**
	 * Creates a new IterationWrapper that operates on the supplied Iteration.
	 *
	 * @param iteration The wrapped Iteration for this <var>IterationWrapper</var>, must not be <var>null</var>.
	 */
	protected CloseableIterationWrapper(T iteration) {
		wrappedIter = Objects.requireNonNull(iteration);
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

	static final Set<Class> classes = Collections.synchronizedSet(new HashSet<>());

	/**
	 * Returns the next element from the wrapped Iteration.
	 *
	 * @throws NoSuchElementException If all elements have been returned or it has been closed.
	 */
	@Override
	public E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}

//		boolean add = classes.add(wrappedIter.getClass());
//		if (add) {
//			System.out.println(wrappedIter.getClass());
//		}

		return wrappedIter.next();
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
		wrappedIter.close();
	}
}
