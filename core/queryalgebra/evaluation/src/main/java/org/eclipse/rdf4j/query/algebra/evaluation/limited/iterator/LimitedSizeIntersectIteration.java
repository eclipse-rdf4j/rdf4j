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

import org.eclipse.rdf4j.common.iteration.IntersectIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
public class LimitedSizeIntersectIteration extends IntersectIteration<BindingSet, QueryEvaluationException> {

	private final AtomicLong used;

	private final long maxSize;

	/**
	 * Creates a new IntersectIteration that returns the intersection of the
	 * results of two Iterations. By default, duplicates are <em>not</em>
	 * filtered from the results.
	 * 
	 * @param arg1
	 *        An Iteration containing the first set of elements.
	 * @param arg2
	 *        An Iteration containing the second set of elements.
	 * @param used
	 *        An atomic long used to monitor how many elements are in the set
	 *        collections.
	 * @param maxSize
	 *        Maximum size allowed by the sum of all collections used by the
	 *        LimitedSizeQueryEvaluatlion.
	 */
	public LimitedSizeIntersectIteration(
			Iteration<? extends BindingSet, ? extends QueryEvaluationException> arg1,
			Iteration<? extends BindingSet, ? extends QueryEvaluationException> arg2, AtomicLong used,
			long maxSize)
	{
		this(arg1, arg2, false, used, maxSize);

	}

	public LimitedSizeIntersectIteration(
			Iteration<? extends BindingSet, ? extends QueryEvaluationException> arg1,
			Iteration<? extends BindingSet, ? extends QueryEvaluationException> arg2, boolean distinct,
			AtomicLong used, long maxSize)
	{
		super(arg1, arg2, distinct);
		this.used = used;
		this.maxSize = maxSize;
	}

	@Override
	public Set<BindingSet> addSecondSet(
			Iteration<? extends BindingSet, ? extends QueryEvaluationException> arg2, Set<BindingSet> set)
		throws QueryEvaluationException
	{

		LimitedSizeIteratorUtil.addAll(arg2, set, used, maxSize);
		return set;
	}

	/**
	 * After closing the set is cleared and any "used" capacity for collections
	 * is returned.
	 */
	@Override
	protected void handleClose()
		throws QueryEvaluationException
	{
		
		long size = clearIncludeSet();
		used.addAndGet(-size);
		super.handleClose();
	}

}
