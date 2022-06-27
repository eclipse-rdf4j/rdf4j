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
@Deprecated(since = "4.1.0")
public class LimitIteration<E, X extends Exception> extends IterationWrapper<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

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
	 * @param iter  The underlying Iteration, must not be <var>null</var>.
	 * @param limit The number of query answers to return, must be &gt;= 0.
	 */
	public LimitIteration(Iteration<? extends E, X> iter, long limit) {
		super(iter);

		assert iter != null;
		assert limit >= 0;

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
		return super.hasNext();
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
		return super.next();
	}
}
