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
 * A CloseableIteration that wraps another Iteration, applying a filter on the objects that are returned. Subclasses
 * must implement the <tt>accept</tt> method to indicate which objects should be returned.
 */
public abstract class FilterIteration<E, X extends Exception> extends IterationWrapper<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile E nextElement;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * @param iter
	 */
	public FilterIteration(Iteration<? extends E, ? extends X> iter) {
		super(iter);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean hasNext() throws X {
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
			while (!isClosed() && nextElement == null && super.hasNext()) {
				E candidate = super.next();

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
	 * @return <tt>true</tt> if the object should be returned, <tt>false</tt> otherwise.
	 * @throws X
	 */
	protected abstract boolean accept(E object) throws X;

	@Override
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			nextElement = null;
		}
	}
}
