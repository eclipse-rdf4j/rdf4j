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
 * the <var>java.util.Iteration</var> interface.
 */
public abstract class LookAheadIteration<E, X extends Exception> implements CloseableIteration<E, X> {

	private E nextElement;
	private boolean closed = false;

	/**
	 * Gets the next element. Subclasses should implement this method so that it returns the next element.
	 *
	 * @implNote Implementations of this method should be effectively final to ensure that the JVM can optimize this to
	 *           a monomorphic call. If you override another class's implementation of this method you will force the
	 *           JVM to use a vtable stub.
	 *
	 * @return The next element, or <var>null</var> if no more elements are available.
	 */
	protected abstract E getNextElement() throws X;

	@Override
	public final boolean hasNext() throws X {
		if (closed) {
			return false;
		}
		if (nextElement == null) {
			lookAhead();
		}
		return nextElement != null;
	}

	@Override
	public final E next() throws X {
		if (closed) {
			throw new NoSuchElementException("The iteration has been closed.");
		}

		if (nextElement == null) {
			lookAhead();
		}

		if (nextElement != null) {
			E temp = nextElement;
			nextElement = null;
			return temp;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Fetches the next element if it hasn't been fetched yet and stores it in {@link #nextElement}.
	 *
	 * @return The next element, or null if there are no more results.
	 * @throws X If there is an issue getting the next element or closing the iteration.
	 */
	private void lookAhead() throws X {
		nextElement = getNextElement();
		if (nextElement == null) {
			close();
		}
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure the resource closures are only executed once.
	 */
	@Override
	public final void close() throws X {
		if (!closed) {
			closed = true;
			handleClose();
		}
	}

	/**
	 * Called by {@link #close} when it is called for the first time. This method is only called once on each iteration.
	 * By default, this method does nothing.
	 *
	 * @throws X
	 */
	abstract protected void handleClose() throws X;

	@Override
	public final boolean isClosed() {
		return closed;
	}
}
