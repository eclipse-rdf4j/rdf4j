/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

/**
 * An Iteration that skips the first <var>offset</var> elements from an underlying Iteration.
 */
@Deprecated(since = "4.1.0")
public class OffsetIteration<E, X extends Exception> extends FilterIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The offset (0-based) of the first element to return.
	 */
	private final long offset;

	/**
	 * The number of elements that have been dropped so far.
	 */
	private long droppedResults;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new OffsetIteration.
	 *
	 * @param iter   The underlying Iteration, must not be <var>null</var>.
	 * @param offset The number of elements to skip, must be larger than or equal to 0.
	 */
	public OffsetIteration(Iteration<? extends E, X> iter, long offset) {
		super(iter);

		assert offset >= 0;

		this.offset = offset;
		this.droppedResults = 0;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Returns <var>false</var> for the first OFFSET objects.
	 */
	@Override
	protected boolean accept(E object) {
		if (droppedResults < offset) {
			droppedResults++;
			return false;
		} else {
			return true;
		}
	}
}
