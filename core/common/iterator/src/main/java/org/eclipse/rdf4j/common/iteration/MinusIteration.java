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
 * An Iteration that returns the results of an Iteration (the left argument) minus the results of another Iteration (the
 * right argument). Optionally, the Iteration can be configured to filter duplicates from the returned elements.
 * <p>
 * Note that duplicates can also be filtered by wrapping this Iteration in a {@link DistinctIteration}, but that has a
 * bit more overhead as it adds a second hash table lookup.
 */
public class MinusIteration<E> extends FilterIteration<E> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<? extends E> rightArg;

	private final boolean distinct;

	private boolean initialized;

	private Set<E> excludeSet;

	private final Supplier<Set<E>> setMaker;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MinusIteration that returns the results of the left argument minus the results of the right
	 * argument. By default, duplicates are <em>not</em> filtered from the results.
	 *
	 * @param leftArg  An Iteration containing the main set of elements.
	 * @param rightArg An Iteration containing the set of elements that should be filtered from the main set.
	 */
	public MinusIteration(CloseableIteration<? extends E> leftArg, CloseableIteration<? extends E> rightArg) {
		this(leftArg, rightArg, false);
	}

	/**
	 * Creates a new MinusIteration that returns the results of the left argument minus the results of the right
	 * argument.
	 *
	 * @param leftArg  An Iteration containing the main set of elements.
	 * @param rightArg An Iteration containing the set of elements that should be filtered from the main set.
	 * @param distinct Flag indicating whether duplicate elements should be filtered from the result.
	 */
	public MinusIteration(CloseableIteration<? extends E> leftArg, CloseableIteration<? extends E> rightArg,
			boolean distinct) {
		super(leftArg);

		assert rightArg != null;

		this.rightArg = rightArg;
		this.distinct = distinct;
		this.initialized = false;
		this.setMaker = HashSet::new;
	}

	/**
	 * Creates a new MinusIteration that returns the results of the left argument minus the results of the right
	 * argument.
	 *
	 * @param leftArg  An Iteration containing the main set of elements.
	 * @param rightArg An Iteration containing the set of elements that should be filtered from the main set.
	 * @param distinct Flag indicating whether duplicate elements should be filtered from the result.
	 */
	public MinusIteration(CloseableIteration<? extends E> leftArg, CloseableIteration<? extends E> rightArg,
			boolean distinct,
			Supplier<Set<E>> setMaker) {
		super(leftArg);

		assert rightArg != null;

		this.rightArg = rightArg;
		this.distinct = distinct;
		this.initialized = false;
		this.setMaker = setMaker;
	}
	/*--------------*
	 * Constructors *
	 *--------------*/

	// implements LookAheadIteration.getNextElement()
	@Override
	protected boolean accept(E object) {
		if (!initialized) {
			// Build set of elements-to-exclude from right argument
			excludeSet = Iterations.asSet(rightArg);
			initialized = true;
		}

		if (!excludeSet.contains(object)) {
			// Object is part of the result

			if (distinct) {
				// Prevent duplicates from being returned by
				// adding the object to the exclude set
				excludeSet.add(object);
			}

			return true;
		}

		return false;
	}

	@Override
	protected void handleClose() {
		if (rightArg != null) {
			rightArg.close();
		}
	}
}
