/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.limited.iterator;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.SPARQLMinusIteration;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
public class LimitedSizeSPARQLMinusIteration extends SPARQLMinusIteration<QueryEvaluationException> {

	private final AtomicLong used;

	private final long maxSize;

	/**
	 * Creates a new MinusIteration that returns the results of the left argument minus the results of the right
	 * argument. By default, duplicates are <em>not</em> filtered from the results.
	 *
	 * @param leftArg  An Iteration containing the main set of elements.
	 * @param rightArg An Iteration containing the set of elements that should be filtered from the main set. * @param
	 *                 used An atomic long used to monitor how many elements are in the set collections.
	 * @param used     An atomic long used to monitor how many elements are in the set collections.
	 * @param maxSize  Maximum size allowed by the sum of all collections used by the LimitedSizeQueryEvaluatlion.
	 */
	public LimitedSizeSPARQLMinusIteration(Iteration<BindingSet, QueryEvaluationException> leftArg,
			Iteration<BindingSet, QueryEvaluationException> rightArg, AtomicLong used, long maxSize) {
		this(leftArg, rightArg, false, used, maxSize);
	}

	/**
	 * Creates a new SPARQLMinusIteration that returns the results of the left argument minus the results of the right
	 * argument.
	 *
	 * @param leftArg  An Iteration containing the main set of elements.
	 * @param rightArg An Iteration containing the set of elements that should be filtered from the main set.
	 * @param distinct Flag indicating whether duplicate elements should be filtered from the result.
	 * @param used     An atomic long used to monitor how many elements are in the set collections.
	 * @param maxSize  Maximum size allowed by the sum of all collections used by the LimitedSizeQueryEvaluatlion.
	 */
	public LimitedSizeSPARQLMinusIteration(Iteration<BindingSet, QueryEvaluationException> leftArg,
			Iteration<BindingSet, QueryEvaluationException> rightArg, boolean distinct, AtomicLong used, long maxSize) {
		super(leftArg, rightArg, distinct);
		this.used = used;
		this.maxSize = maxSize;
	}

	@Override
	protected Set<BindingSet> makeSet(Iteration<BindingSet, QueryEvaluationException> rightArg2)
			throws QueryEvaluationException {
		return LimitedSizeIteratorUtil.addAll(rightArg2, makeSet(), used, maxSize);
	}

	/**
	 * After closing the set is cleared and any "used" capacity for collections is returned.
	 */
	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			super.handleClose();
		} finally {
			long size = clearExcludeSet();
			used.addAndGet(-size);
		}
	}

}
