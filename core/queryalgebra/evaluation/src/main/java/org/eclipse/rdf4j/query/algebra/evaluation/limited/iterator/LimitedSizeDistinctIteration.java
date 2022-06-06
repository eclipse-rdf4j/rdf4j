/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.limited.iterator;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
@Deprecated(since = "4.1.0")
public class LimitedSizeDistinctIteration extends DistinctIteration<BindingSet, QueryEvaluationException> {

	private final AtomicLong used;

	private final long maxSize;

	/**
	 * @param iter
	 */
	public LimitedSizeDistinctIteration(Iteration<? extends BindingSet, ? extends QueryEvaluationException> iter,
			AtomicLong used, long maxSize) {
		super(iter);
		this.used = used;
		this.maxSize = maxSize;
	}

	@Override
	protected boolean add(BindingSet object) throws QueryEvaluationException {
		boolean add = super.add(object);
		if (add && used.incrementAndGet() > maxSize) {
			throw new QueryEvaluationException("Size limited reached inside query operator.");
		}
		return add;
	}

}
