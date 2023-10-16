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
public class CloseableIteratorIteration<E> implements CloseableIteration<E> {

	private Iterator<? extends E> iter;
	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private boolean closed = false;

	/**
	 * Creates a CloseableIteratorIteration that wraps the supplied iterator.
	 */
	public CloseableIteratorIteration(Iterator<? extends E> iter) {
		this.iter = Objects.requireNonNull(iter, "Iterator was null");
	}

	@Override
	public boolean hasNext() {
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
	public E next() {
		if (isClosed()) {
			throw new NoSuchElementException("Iteration has been closed");
		}

		return iter.next();
	}

	@Override
	public void remove() {
		if (isClosed()) {
			throw new IllegalStateException("Iteration has been closed");
		}

		iter.remove();
	}

	protected void handleClose() {

	}

	/**
	 * Checks whether this CloseableIteration has been closed.
	 *
	 * @return <var>true</var> if the CloseableIteration has been closed, <var>false</var> otherwise.
	 */
	public final boolean isClosed() {
		return closed;
	}

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure the resource closures are only executed once.
	 */
	@Override
	public final void close() {
		if (!closed) {
			closed = true;
			handleClose();
		}
	}
}
