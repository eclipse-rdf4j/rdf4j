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
package org.eclipse.rdf4j.query.algebra.evaluation.limited.iterator;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
@Deprecated(since = "4.1.0", forRemoval = true)
public class LimitedSizeIteratorUtil {

	/**
	 * @param arg2       the iteration with elements to add to the includeSet
	 * @param includeSet the set that should have all unique elements of arg2
	 * @param used       the collection size counter of all collections used in answering a query
	 * @param maxSize    the point at which we throw a new query exception
	 * @return the includeSet
	 * @throws QueryEvaluationException trigerred when maxSize is smaller than the used value
	 */
	public static Set<BindingSet> addAll(Iteration<? extends BindingSet, ? extends QueryEvaluationException> arg2,
			Set<BindingSet> includeSet, AtomicLong used, long maxSize) throws QueryEvaluationException {
		while (arg2.hasNext()) {
			if (includeSet.add(arg2.next()) && used.incrementAndGet() > maxSize) {
				throw new QueryEvaluationException("Size limited reached inside intersect operator");
			}
		}
		return includeSet;
	}

	/**
	 * @param object     object to put in set if not there already.
	 * @param excludeSet set that we need to store object in.
	 * @param used       AtomicLong tracking how many elements we have in storage.
	 * @param maxSize
	 * @throws QueryEvaluationException when the object is added to the set and the total elements in all limited size
	 *                                  collections exceed the allowed maxSize.
	 */
	public static <V> boolean add(V object, Collection<V> excludeSet, AtomicLong used, long maxSize)
			throws QueryEvaluationException {
		boolean add = excludeSet.add(object);
		if (add && used.incrementAndGet() > maxSize) {
			throw new QueryEvaluationException("Size limited reached inside query operator.");
		}
		return add;
	}
}
