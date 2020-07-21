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
 * A CloseableIteration that converts an arbitrary iteration to an iteration with exceptions of type <tt>X</tt>.
 * Subclasses need to override {@link #convert(Exception)} to do the conversion.
 */
public abstract class ExceptionConvertingIteration<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The underlying Iteration.
	 */
	private final Iteration<? extends E, ? extends Exception> iter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new ExceptionConvertingIteration that operates on the supplied iteration.
	 *
	 * @param iter The Iteration that this <tt>ExceptionConvertingIteration</tt> operates on, must not be <tt>null</tt>.
	 */
	protected ExceptionConvertingIteration(Iteration<? extends E, ? extends Exception> iter) {
		this.iter = Objects.requireNonNull(iter, "The iterator was null");
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Converts an exception from the underlying iteration to an exception of type <tt>X</tt>.
	 */
	protected abstract X convert(Exception e);

	/**
	 * Checks whether the underlying Iteration contains more elements.
	 *
	 * @return <tt>true</tt> if the underlying Iteration contains more elements, <tt>false</tt> otherwise.
	 * @throws X
	 */
	@Override
	public boolean hasNext() throws X {
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
			throw convert(e);
		}
	}

	/**
	 * Returns the next element from the wrapped Iteration.
	 *
	 * @throws X
	 * @throws java.util.NoSuchElementException If all elements have been returned.
	 * @throws IllegalStateException            If the Iteration has been closed.
	 */
	@Override
	public E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		try {
			return iter.next();
		} catch (NoSuchElementException e) {
			close();
			throw e;
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw convert(e);
		}
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
		try {
			iter.remove();
		} catch (UnsupportedOperationException | IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw convert(e);
		}
	}

	/**
	 * Closes this Iteration as well as the wrapped Iteration if it happens to be a {@link CloseableIteration} .
	 */
	@Override
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			try {
				Iterations.closeCloseable(iter);
			} catch (Exception e) {
				throw convert(e);
			}
		}
	}
}
