/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.limited.iterator;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
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
			Comparator<BindingSet> comparator, AtomicLong used, long maxSize)
	{
		this(iter, comparator, Integer.MAX_VALUE, false, used, maxSize);
	}

	public LimitedSizeOrderIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			Comparator<BindingSet> comparator, long limit, boolean distinct, AtomicLong used, long maxSize)
	{
		super(iter, comparator, limit, distinct);
		this.used = used;
		this.maxSize = maxSize;
	}

	@Override
	protected void removeLast(Collection<BindingSet> lastResults) {
		super.removeLast(lastResults);
		used.decrementAndGet();
	}

	@Override
	protected boolean add(BindingSet next, Collection<BindingSet> list)
		throws QueryEvaluationException
	{

		return LimitedSizeIteratorUtil.add(next, list, used, maxSize);
	}

	@Override
	protected Integer put(NavigableMap<BindingSet, Integer> map, BindingSet next, int count)
		throws QueryEvaluationException
	{
		final Integer i = map.get(next);
		final int oldCount = i == null ? 0 : i;
		
		final Integer put = super.put(map, next, count);

		if (oldCount < count) {
			if (used.incrementAndGet() > maxSize) {
				throw new QueryEvaluationException(
						"Size limited reached inside order operator query, max size is:" + maxSize);
			}
		}
		else if (oldCount > count) {
			used.decrementAndGet();
		}
		
		return put;
	}

}
