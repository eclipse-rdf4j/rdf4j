/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.limited.iterator;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.OrderIterator;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
public class LimitedSizeOrderIteration extends OrderIterator {

	private final AtomicLong used;

	private final long maxSize;

	/**
	 * @param iter
	 * @param comparator
	 */
	public LimitedSizeOrderIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			Comparator<BindingSet> comparator, AtomicLong used, long maxSize) {
		this(iter, comparator, Integer.MAX_VALUE, false, used, maxSize);
	}

	public LimitedSizeOrderIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			Comparator<BindingSet> comparator, long limit, boolean distinct, AtomicLong used, long maxSize) {
		super(iter, comparator, limit, distinct);
		this.used = used;
		this.maxSize = maxSize;
	}

	@Override
	protected void increment() throws QueryEvaluationException {
		if (used.incrementAndGet() > maxSize) {
			throw new QueryEvaluationException(
					"Size limited reached inside order operator query, max size is:" + maxSize);
		}
	}

	@Override
	protected void decrement(int amount) throws QueryEvaluationException {
		used.getAndAdd(-amount);
	}

}
