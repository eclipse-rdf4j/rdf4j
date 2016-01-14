/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.limited.iterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.BindingSetHashKey;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.HashJoinIteration;


/**
 *
 * @author MJAHale
 */
public class LimitedSizeHashJoinIteration extends HashJoinIteration {
	private static final String SIZE_LIMIT_REACHED = "Size limited reached inside bottom up join operator, max size is:";
	private AtomicLong used;

	private long maxSize;

	public LimitedSizeHashJoinIteration(EvaluationStrategy limitedSizeEvaluationStrategy,
			Join join, BindingSet bindings, AtomicLong used, long maxSize)
			throws QueryEvaluationException
	{
		super(limitedSizeEvaluationStrategy, join, bindings);
		this.used = used;
		this.maxSize = maxSize;
	}


	protected <E> E nextFromCache(Iterator<E> iter)
	{
		E v = iter.next();
		used.decrementAndGet();
		iter.remove();
		return v;
	}

	protected <E> void add(Collection<E> col, E value)
		throws QueryEvaluationException
	{
		if (col.add(value) && used.incrementAndGet() > maxSize) {
			throw new QueryEvaluationException(SIZE_LIMIT_REACHED+maxSize);
		}
	}

	protected <E> void addAll(Collection<E> col, List<E> values)
		throws QueryEvaluationException
	{
		for (E v : values) {
			add(col, v);
		}
	}

	protected void putHashTableEntry(Map<BindingSetHashKey, List<BindingSet>> hashTable, BindingSetHashKey hashKey,
			List<BindingSet> hashValue)
		throws QueryEvaluationException
	{
		List<BindingSet> put = hashTable.put(hashKey, hashValue);
		if (put == null && used.incrementAndGet() > maxSize) {
			throw new QueryEvaluationException(SIZE_LIMIT_REACHED+maxSize);
		}
	}

	@Override
	protected void disposeHashTable(Map<BindingSetHashKey, List<BindingSet>> map)
	{
		long htvSize = map.size();
		map.clear();
		used.addAndGet(-htvSize);
	}

}
