/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.limited.iterator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.BottomUpJoinIterator;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 * @deprecated replaced by LimitedSizeHashJoinIteration
 */
@Deprecated
public class LimitedSizeBottomUpJoinIterator extends BottomUpJoinIterator {

	private static final String SIZE_LIMIT_REACHED = "Size limited reached inside bottom up join operator, max size is:";
	private AtomicLong used;

	private long maxSize;

	/**
	 * @param limitedSizeEvaluationStrategy
	 * @param join
	 * @param bindings
	 * @param used
	 * @param maxSize
	 * @throws QueryEvaluationException
	 */
	public LimitedSizeBottomUpJoinIterator(EvaluationStrategy limitedSizeEvaluationStrategy,
			Join join, BindingSet bindings, AtomicLong used, long maxSize)
		throws QueryEvaluationException
	{
		super(limitedSizeEvaluationStrategy, join, bindings);
		this.used = used;
		this.maxSize = maxSize;
	}

	@Override
	protected void addAll(List<BindingSet> hashTableValues, List<BindingSet> values)
		throws QueryEvaluationException
	{
		Iterator<BindingSet> iter = values.iterator();
		while (iter.hasNext()) {
			if (hashTableValues.add(iter.next()) && used.incrementAndGet() > maxSize) {
				throw new QueryEvaluationException(SIZE_LIMIT_REACHED+maxSize);
			}
		}
	}

	@Override
	protected void add(List<BindingSet> leftArgResults, BindingSet b)
		throws QueryEvaluationException
	{
		if (leftArgResults.add(b) && used.incrementAndGet() > maxSize) {
			throw new QueryEvaluationException(SIZE_LIMIT_REACHED+maxSize);
		}
	}

	@Override
	protected BindingSet removeFirstElement(List<BindingSet> list)
		throws QueryEvaluationException
	{
		used.decrementAndGet();
		return super.removeFirstElement(list);
	}

	@Override
	protected void put(Map<BindingSet, List<BindingSet>> hashTable, BindingSet hashKey,
			List<BindingSet> hashValue)
		throws QueryEvaluationException
	{
		List<BindingSet> put = hashTable.put(hashKey, hashValue);
		if (put == null && used.incrementAndGet() > maxSize) {
			throw new QueryEvaluationException(SIZE_LIMIT_REACHED+maxSize);
		}
	}

	@Override
	protected void handleClose()
		throws QueryEvaluationException
	{
		long htvSize = clearHashTable();
		super.handleClose();
		used.addAndGet(-htvSize);
	}

	

}
