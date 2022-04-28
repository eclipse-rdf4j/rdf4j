/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;

/**
 * An Iteration that limits the amount of elements that it returns from an underlying Iteration to a fixed amount. This
 * class returns the first <var>limit</var> elements from the underlying Iteration and drops the rest.
 */
public class LimitIteration<K extends CloseableIteration<E, X>, E, X extends Exception>
		implements CloseableIteration<E, X> {

	private final K delegate;

	/**
	 * The amount of elements to return.
	 */
	private final long limit;

	/**
	 * The number of elements that have been returned so far.
	 */
	private long returnCount;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new LimitIteration.
	 *
	 * @param delegate The underlying Iteration, must not be <var>null</var>.
	 * @param limit    The number of query answers to return, must be &gt;= 0.
	 */
	public LimitIteration(K delegate, long limit) {
		assert delegate != null;
		assert limit >= 0;

		this.delegate = delegate;
		this.limit = limit;
		this.returnCount = 0;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean hasNext() throws X {
		if (isClosed()) {
			return false;
		}
		boolean underLimit = returnCount < limit;
		if (!underLimit) {
			close();
			return false;
		}
		return delegate.hasNext();
	}

	@Override
	public E next() throws X {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		if (returnCount >= limit) {
			close();
			throw new NoSuchElementException("limit reached");
		}

		returnCount++;
		return delegate.next();
	}

	@Override
	public void close() throws X {
		delegate.close();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public void remove() throws X {
		delegate.remove();
	}
}
