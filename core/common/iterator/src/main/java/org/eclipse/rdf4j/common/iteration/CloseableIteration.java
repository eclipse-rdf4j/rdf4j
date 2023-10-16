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
import java.util.stream.Stream;

/**
 * An {@link CloseableIteration} that can be closed to free resources that it is holding. CloseableIterations
 * automatically free their resources when exhausted. If not read until exhaustion or if you want to make sure the
 * iteration is properly closed, any code using the iterator should be placed in a try-with-resources block, closing the
 * iteration automatically, e.g.:
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
 */
public interface CloseableIteration<E> extends Iterator<E>, AutoCloseable {

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
	void close();

}
