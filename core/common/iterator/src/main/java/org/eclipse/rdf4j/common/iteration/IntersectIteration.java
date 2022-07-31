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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * An Iteration that returns the intersection of the results of two Iterations. Optionally, the Iteration can be
 * configured to filter duplicates from the returned elements.
 * <p>
 * Note that duplicates can also be filtered by wrapping this Iteration in a {@link DistinctIteration}, but that has a
 * bit more overhead as it adds a second hash table lookup.
 */
@Deprecated(since = "4.1.0")
public class IntersectIteration<E, X extends Exception> extends FilterIteration<E, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	protected final Iteration<? extends E, ? extends X> arg2;

	private final boolean distinct;

	private boolean initialized;

	private Set<E> includeSet;

	private final Supplier<Set<E>> setMaker;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new IntersectIteration that returns the intersection of the results of two Iterations. By default,
	 * duplicates are <em>not</em> filtered from the results.
	 *
	 * @param arg1 An Iteration containing the first set of elements.
	 * @param arg2 An Iteration containing the second set of elements.
	 */
	public IntersectIteration(Iteration<? extends E, ? extends X> arg1, Iteration<? extends E, ? extends X> arg2) {
		this(arg1, arg2, false);
	}

	public IntersectIteration(Iteration<? extends E, ? extends X> arg1, Iteration<? extends E, ? extends X> arg2,
			Supplier<Set<E>> setMaker) {
		this(arg1, arg2, false, setMaker);
	}

	/**
	 * Creates a new IntersectIteration that returns the intersection of the results of two Iterations.
	 *
	 * @param arg1     An Iteration containing the first set of elements.
	 * @param arg2     An Iteration containing the second set of elements.
	 * @param distinct Flag indicating whether duplicate elements should be filtered from the result.
	 */
	public IntersectIteration(Iteration<? extends E, ? extends X> arg1, Iteration<? extends E, ? extends X> arg2,
			boolean distinct) {
		super(arg1);

		assert arg2 != null;

		this.arg2 = arg2;
		this.distinct = distinct;
		this.initialized = false;
		this.setMaker = this::makeSet;
	}

	/**
	 * Creates a new IntersectIteration that returns the intersection of the results of two Iterations.
	 *
	 * @param arg1     An Iteration containing the first set of elements.
	 * @param arg2     An Iteration containing the second set of elements.
	 * @param distinct Flag indicating whether duplicate elements should be filtered from the result.
	 */
	public IntersectIteration(Iteration<? extends E, ? extends X> arg1, Iteration<? extends E, ? extends X> arg2,
			boolean distinct, Supplier<Set<E>> setMaker) {
		super(arg1);

		assert arg2 != null;

		this.arg2 = arg2;
		this.distinct = distinct;
		this.initialized = false;
		this.setMaker = setMaker;
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Returns <var>true</var> if the object is in the set of elements of the second argument.
	 */
	@Override
	protected boolean accept(E object) throws X {
		if (!initialized) {
			// Build set of elements-to-include from second argument
			includeSet = Iterations.asSet(arg2);
			initialized = true;
		}

		if (inIncludeSet(object)) {
			// Element is part of the result

			if (distinct) {
				// Prevent duplicates from being returned by
				// removing the element from the include set
				removeFromIncludeSet(object);
			}

			return true;
		}

		return false;
	}

	// this method does not seem to "addSecondSet" since the second set seems to be ignored
	public Set<E> addSecondSet(Iteration<? extends E, ? extends X> arg2, Set<E> set) throws X {
		return Iterations.addAll(arg2, setMaker.get());
	}

	protected boolean removeFromIncludeSet(E object) {
		return includeSet.remove(object);
	}

	protected boolean inIncludeSet(E object) {
		return includeSet.contains(object);
	}

	protected Set<E> makeSet() {
		return new HashSet<>();
	}

	@Override
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			Iterations.closeCloseable(arg2);
		}
	}

	protected long clearIncludeSet() {
		long size = includeSet.size();
		includeSet.clear();
		return size;
	}

}
