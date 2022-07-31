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
package org.eclipse.rdf4j.query;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iterator.CloseableIterationIterator;

/**
 * Super type of all query result types (TupleQueryResult, GraphQueryResult, etc.).
 *
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 */
public interface QueryResult<T>
		extends AutoCloseable, CloseableIteration<T, QueryEvaluationException>, Iterable<T> {

	@Override
	default Iterator<T> iterator() {
		return new CloseableIterationIterator<>(this);
	}

	/**
	 * Returns {@code true} if the query result has more elements. (In other words, returns {@code true} if
	 * {@link #next} would return an element rather than throwing a {@link NoSuchElementException}.)
	 *
	 * @return {@code true} if the iteration has more elements.
	 * @throws QueryEvaluationException if an error occurs while executing the query.
	 */
	boolean hasNext() throws QueryEvaluationException;

	/**
	 * Returns the next element in the query result.
	 *
	 * @return the next element in the query result.
	 * @throws NoSuchElementException   if the iteration has no more elements or if it has been closed.
	 * @throws QueryEvaluationException if an error occurs while executing the query.
	 */
	T next() throws QueryEvaluationException;

	/**
	 *
	 * Convert the result elements to a Java {@link Stream}. Note that the consumer should take care to close the stream
	 * (by calling Stream#close() or using try-with-resource) if it is not fully consumed.
	 *
	 * @return stream a {@link Stream} of query result elements.
	 */
	default Stream<T> stream() {
		return QueryResults.stream(this);
	}

}
