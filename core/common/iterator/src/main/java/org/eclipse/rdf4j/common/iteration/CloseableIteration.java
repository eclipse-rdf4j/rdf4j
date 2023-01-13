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
 * An {@link Iteration} that can be closed to free resources that it is holding. CloseableIterations automatically free
 * their resources when exhausted. If not read until exhaustion or if you want to make sure the iteration is properly
 * closed, any code using the iterator should be placed in a try-with-resources block, closing the iteration
 * automatically, e.g.:
 *
 * <pre>
 *
 * try (CloseableIteration&lt;Object, Exception&gt; iter = ...) {
 *    // read objects from the iterator
 * }
 * catch(Exception e) {
 *   // process the exception that can be thrown while processing.
 * }
 * </pre>
 *
 * @deprecated In the future this interface will stop extending {@link Iteration} and instead declare the same interface
 *             methods directly. The interface will also stop requiring implementations to automatically close when
 *             exhausted, instead making this an optional feature and requiring the user to always call close. This
 *             interface may also be removed.
 */
//@Deprecated(since = "4.1.0")
public interface CloseableIteration<E, X extends Exception> extends AutoCloseable {

	/**
	 * Convert the results to a Java 8 Stream.
	 *
	 * @return stream
	 */
	default Stream<E> stream() {
		return Iterations.stream(this);
	}

	/**
	 * Closes this iteration, freeing any resources that it is holding. If the iteration has already been closed then
	 * invoking this method has no effect.
	 */
	@Override
	void close() throws X;

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

}
