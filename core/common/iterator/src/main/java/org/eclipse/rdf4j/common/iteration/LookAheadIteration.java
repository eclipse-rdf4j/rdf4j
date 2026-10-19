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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Iteration that looks one element ahead, if necessary, to handle calls to {@link #hasNext}. This is a convenient
 * super class for Iterations that have no easy way to tell if there are any more results, but still should implement
 * the <var>java.util.Iteration</var> interface.
 */
public abstract class LookAheadIteration<E> extends AbstractCloseableIteration<E> {
	private static final Logger log = LoggerFactory.getLogger(LookAheadIteration.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	private E nextElement;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected LookAheadIteration() {
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the next element. Subclasses should implement this method so that it returns the next element.
	 *
	 * @return The next element, or <var>null</var> if no more elements are available.
	 */
	protected abstract E getNextElement();

	@Override
	public final boolean hasNext() {
		if (isClosed()) {
			return false;
		}

		if (Thread.currentThread().isInterrupted()) {
			log.debug("Thread {} is interrupted, closing iteration", Thread.currentThread().getName());
			close();
			return false;
		}

		try {
			return lookAhead() != null;
		} catch (NoSuchElementException logged) {
			// The lookAhead() method shouldn't throw a NoSuchElementException since it should return null when there
			// are no more elements.
			log.trace("LookAheadIteration threw NoSuchElementException:", logged);
			return false;
		}
	}

	@Override
	public final E next() {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		E result = lookAhead();

		if (result != null) {
			nextElement = null;
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Fetches the next element if it hasn't been fetched yet and stores it in {@link #nextElement}.
	 *
	 * @return The next element, or null if there are no more results.
	 */
	private E lookAhead() {
		if (nextElement == null) {
			nextElement = getNextElement();

			if (nextElement == null) {
				close();
			}
		}
		return nextElement;
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
