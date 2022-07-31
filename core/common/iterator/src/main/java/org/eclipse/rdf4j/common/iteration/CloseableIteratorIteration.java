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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An Iteration that can convert an {@link Iterator} to a {@link CloseableIteration}.
 */
@Deprecated(since = "4.1.0")
public class CloseableIteratorIteration<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

	private Iterator<? extends E> iter;

	/**
	 * Creates an uninitialized CloseableIteratorIteration, needs to be initialized by calling
	 * {@link #setIterator(Iterator)} before it can be used.
	 */
	public CloseableIteratorIteration() {
	}

	/**
	 * Creates a CloseableIteratorIteration that wraps the supplied iterator.
	 */
	public CloseableIteratorIteration(Iterator<? extends E> iter) {
		setIterator(iter);
	}

	protected void setIterator(Iterator<? extends E> iter) {
		this.iter = Objects.requireNonNull(iter, "Iterator was null");
	}

	protected boolean hasIterator() {
		return iter != null;
	}

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

	@Override
	public E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("Iteration has been closed");
		}

		return iter.next();
	}

	@Override
	public void remove() throws X {
		if (isClosed()) {
			throw new IllegalStateException("Iteration has been closed");
		}

		iter.remove();
	}
}
