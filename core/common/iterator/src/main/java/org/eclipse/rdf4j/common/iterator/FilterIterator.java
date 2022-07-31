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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A CloseableIterator that wraps another iterator, applying a filter on the objects that are returned. Subclasses must
 * implement the <var>accept</var> method to indicate which objects should be returned.
 */
public abstract class FilterIterator<E> implements Iterator<E> {

	private final Iterator<? extends E> filteredIter;

	private E nextElement;

	protected FilterIterator(Iterator<? extends E> iter) {
		this.filteredIter = iter;
	}

	@Override
	public boolean hasNext() {
		findNextElement();

		return nextElement != null;
	}

	@Override
	public E next() {
		findNextElement();

		E result = nextElement;

		if (result != null) {
			nextElement = null;
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	private void findNextElement() {
		while (nextElement == null && filteredIter.hasNext()) {
			E candidate = filteredIter.next();

			if (accept(candidate)) {
				nextElement = candidate;
			}
		}
	}

	@Override
	public void remove() {
		filteredIter.remove();
	}

	/**
	 * Tests whether or not the specified object should be returned by this iterator. All objects from the wrapped
	 * iterator pass through this method in the same order as they are coming from the wrapped iterator.
	 *
	 * @param object The object to be tested.
	 * @return <var>true</var> if the object should be returned, <var>false</var> otherwise.
	 * @throws X
	 */
	protected abstract boolean accept(E object);
}
