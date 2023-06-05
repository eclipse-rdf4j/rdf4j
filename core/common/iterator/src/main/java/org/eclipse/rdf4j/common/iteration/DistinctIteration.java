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
 * An Iteration that filters any duplicate elements from an underlying iterator.
 */
public class DistinctIteration<E> extends FilterIteration<E> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The elements that have already been returned.
	 */
	private final Set<E> excludeSet;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new DistinctIterator.
	 *
	 * @param iter The underlying iterator.
	 */
	@Deprecated
	public DistinctIteration(CloseableIteration<? extends E> iter) {
		super(iter);

		excludeSet = new HashSet<>();
	}

	/**
	 * Creates a new DistinctIterator.
	 *
	 * @param Set<E> a hopefully optimized set
	 * @param iter   The underlying iterator.
	 */
	public DistinctIteration(CloseableIteration<? extends E> iter, Set<E> excludeSet) {
		super(iter);
		this.excludeSet = excludeSet;
	}

	/**
	 * Creates a new DistinctIterator.
	 *
	 * @param Supplier<Set<E>> a supplier of a hopefully optimized set
	 * @param iter             The underlying iterator.
	 */
	public DistinctIteration(CloseableIteration<? extends E> iter, Supplier<Set<E>> setMaker) {
		super(iter);
		excludeSet = setMaker.get();
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Returns <var>true</var> if the specified object hasn't been seen before.
	 */
	@Override
	protected boolean accept(E object) {
		if (inExcludeSet(object)) {
			// object has already been returned
			return false;
		} else {
			add(object);
			return true;
		}
	}

	@Override
	protected void handleClose() {

	}

	/**
	 * @param object
	 * @return true if the object is in the excludeSet
	 */
	private boolean inExcludeSet(E object) {
		return excludeSet.contains(object);
	}

	/**
	 * @param object to put into the set
	 */
	protected boolean add(E object) {
		return excludeSet.add(object);
	}
}
