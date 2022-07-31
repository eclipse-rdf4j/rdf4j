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

/**
 * An Iterator that converts an iterator over objects of type <var>S</var> (the source type) to an iterator over objects
 * of type <var>T</var> (the target type).
 */
public abstract class ConvertingIterator<S, T> implements Iterator<T> {

	/**
	 * The source type iterator.
	 */
	private final Iterator<? extends S> sourceIter;

	/**
	 * Creates a new ConvertingIterator that operates on the supplied source type itertor.
	 *
	 * @param iter The source type itertor for this <var>ConvertingIterator</var>, must not be <var>null</var>.
	 */
	protected ConvertingIterator(Iterator<? extends S> iter) {
		assert iter != null;
		this.sourceIter = iter;
	}

	/**
	 * Converts a source type object to a target type object.
	 */
	protected abstract T convert(S sourceObject);

	/**
	 * Checks whether the source type itertor contains more elements.
	 *
	 * @return <var>true</var> if the source type itertor contains more elements, <var>false</var> otherwise.
	 */
	@Override
	public boolean hasNext() {
		return sourceIter.hasNext();
	}

	/**
	 * Returns the next element from the source type itertor.
	 *
	 * @throws java.util.NoSuchElementException If all elements have been returned.
	 * @throws IllegalStateException            If the itertor has been closed.
	 */
	@Override
	public T next() {
		return convert(sourceIter.next());
	}

	/**
	 * Calls <var>remove()</var> on the underlying itertor.
	 *
	 * @throws UnsupportedOperationException If the wrapped itertor does not support the <var>remove</var> operation.
	 * @throws IllegalStateException         If the itertor has been closed, or if {@link #next} has not yet been
	 *                                       called, or {@link #remove} has already been called after the last call to
	 *                                       {@link #next}.
	 */
	@Override
	public void remove() {
		sourceIter.remove();
	}
}
