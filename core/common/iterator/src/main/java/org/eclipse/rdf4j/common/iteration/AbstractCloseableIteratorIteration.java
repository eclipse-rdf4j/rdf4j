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

/**
 * An Iteration that can convert an {@link Iterator} to a {@link CloseableIteration}.
 */
public abstract class AbstractCloseableIteratorIteration<E> extends AbstractCloseableIteration<E> {

	private Iterator<? extends E> iter;

	public AbstractCloseableIteratorIteration() {
	}

	protected abstract Iterator<? extends E> getIterator();

	@Override
	public boolean hasNext() {
		if (isClosed()) {
			return false;
		}

		if (iter == null) {
			iter = getIterator();
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

		if (iter == null) {
			iter = getIterator();
		}

		return iter.next();
	}

	@Override
	public void remove() {
		if (isClosed()) {
			throw new IllegalStateException("Iteration has been closed");
		}

		if (iter == null) {
			iter = getIterator();
		}

		iter.remove();
	}

}
