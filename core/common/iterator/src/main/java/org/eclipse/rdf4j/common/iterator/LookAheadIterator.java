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

package org.eclipse.rdf4j.common.iterator;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * @author MJAHale
 */
public abstract class LookAheadIterator<E> extends AbstractCloseableIterator<E> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private E nextElement;

	private IOException closeException;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected LookAheadIterator() {
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
		lookAhead();

		return nextElement != null;
	}

	@Override
	public final E next() {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		lookAhead();

		E result = nextElement;

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
	 * @throws X
	 */
	private void lookAhead() {
		if (nextElement == null && !isClosed()) {
			nextElement = getNextElement();

			if (nextElement == null) {
				try {
					close();
				} catch (IOException ioe) {
					closeException = ioe;
				}
			}
		}
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void handleClose() throws IOException {
		try {
			super.handleClose();
		} finally {
			nextElement = null;
		}
	}

	@Override
	protected void handleAlreadyClosed() throws IOException {
		IOException toThrowException = closeException;
		if (toThrowException != null) {
			throw toThrowException;
		}
	}
}
