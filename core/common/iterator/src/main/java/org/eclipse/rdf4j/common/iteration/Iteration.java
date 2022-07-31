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
import java.util.stream.Stream;

/**
 * An Iteration is a typed Iterator-like object that can throw (typed) Exceptions while iterating. This is used in cases
 * where the iteration is lazy and evaluates over a (remote) connection, for example accessing a database. In such cases
 * an error can occur at any time and needs to be communicated through a checked exception, something
 * {@link java.util.Iterator} can not do (it can only throw {@link RuntimeException}s.
 *
 * @param <E> Object type of objects contained in the iteration.
 * @param <X> Exception type that is thrown when a problem occurs during iteration.
 * @see java.util.Iterator
 * @author jeen
 * @author Herko ter Horst
 * @deprecated For performance and simplification the Iteration interface is deprecated and will be removed in 5.0.0.
 *             Use CloseableIteration instead, even if your iteration doesn't require AutoCloseable.
 */
@Deprecated(since = "4.1.0", forRemoval = true)
public interface Iteration<E, X extends Exception> {

	/**
	 * Returns <var>true</var> if the iteration has more elements. (In other words, returns <var>true</var> if
	 * {@link #next} would return an element rather than throwing a <var>NoSuchElementException</var>.)
	 *
	 * @return <var>true</var> if the iteration has more elements.
	 * @throws X
	 */
	boolean hasNext() throws X;

	/**
	 * Returns the next element in the iteration.
	 *
	 * @return the next element in the iteration.
	 * @throws NoSuchElementException if the iteration has no more elements or if it has been closed.
	 */
	E next() throws X;

	/**
	 * Removes from the underlying collection the last element returned by the iteration (optional operation). This
	 * method can be called only once per call to next.
	 *
	 * @throws UnsupportedOperationException if the remove operation is not supported by this Iteration.
	 * @throws IllegalStateException         If the Iteration has been closed, or if <var>next()</var> has not yet been
	 *                                       called, or <var>remove()</var> has already been called after the last call
	 *                                       to <var>next()</var>.
	 */
	void remove() throws X;

	/**
	 *
	 * Convert the results to a Java 8 Stream. If this iteration implements CloseableIteration it should be closed (by
	 * calling Stream#close() or using try-with-resource) if it is not fully consumed.
	 *
	 * @return stream
	 */
	default Stream<E> stream() {
		return Iterations.stream(this);
	}
}
