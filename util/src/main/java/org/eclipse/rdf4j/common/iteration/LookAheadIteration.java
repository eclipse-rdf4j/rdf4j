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
 * An Iteration that looks one element ahead, if necessary, to handle calls to {@link #hasNext}. This is a convenient
 * super class for Iterations that have no easy way to tell if there are any more results, but still should implement
 * the <tt>java.util.Iteration</tt> interface.
 */
public abstract class LookAheadIteration<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile E nextElement;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LookAheadIteration() {
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the next element. Subclasses should implement this method so that it returns the next element.
	 * 
	 * @return The next element, or <tt>null</tt> if no more elements are available.
	 */
	protected abstract E getNextElement() throws X;

	@Override
	public final boolean hasNext() throws X {
		if (isClosed()) {
			return false;
		}
		boolean result = (lookAhead() != null);
		if (!result) {
			close();
		}
		return result;
	}

	@Override
	public final E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		E result = lookAhead();

		if (result != null) {
			nextElement = null;
			return result;
		} else {
			close();
			throw new NoSuchElementException();
		}
	}

	/**
	 * Fetches the next element if it hasn't been fetched yet and stores it in {@link #nextElement}.
	 * 
	 * @return The next element, or null if there are no more results.
	 * @throws X If there is an issue getting the next element or closing the iteration.
	 */
	private E lookAhead() throws X {
		E checkElement = nextElement;
		if (checkElement == null && !isClosed()) {
			checkElement = nextElement = getNextElement();

			if (checkElement == null) {
				close();
			}
		}
		return checkElement;
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			nextElement = null;
		}
	}
}
