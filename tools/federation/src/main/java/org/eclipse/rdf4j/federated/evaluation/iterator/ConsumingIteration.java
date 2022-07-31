/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import com.google.common.collect.Lists;

/**
 * A specialized {@link CloseableIteration} that consumes part (or the entire input iteration if it fits into the
 * buffer) and keeps data for further processing in memory. If the buffer is full, the remaining items will be read from
 * the iteration lazily.
 *
 * This implementation can be used to avoid blocking behavior in HTTP connection streams, i.e. to process results in
 * memory and close the underlying HTTP stream.
 *
 * @author Andreas Schwarte
 *
 */
public class ConsumingIteration implements CloseableIteration<BindingSet, QueryEvaluationException> {

	private final List<BindingSet> consumed = Lists.newArrayList();

	private final CloseableIteration<BindingSet, QueryEvaluationException> innerIter;

	/**
	 * The index of the next element that will be returned by a call to {@link #next()}.
	 */
	private int currentIndex = 0;

	/**
	 * @param iter iteration to be consumed
	 * @param max  the number of results to be consumed.
	 * @throws QueryEvaluationException
	 */
	public ConsumingIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter, int max)
			throws QueryEvaluationException {

		innerIter = iter;

		boolean completed = false;
		try {
			while (consumed.size() < max && iter.hasNext()) {
				consumed.add(iter.next());
			}
			if (!iter.hasNext()) {
				iter.close();
			}
			completed = true;
		} finally {
			if (!completed) {
				iter.close();
			}
		}

	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return currentIndex < consumed.size() || innerIter.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		if (hasNext()) {
			// try to read from the consumed items
			if (currentIndex < consumed.size()) {
				BindingSet result = consumed.get(currentIndex);
				currentIndex++;
				return result;
			}
			return innerIter.next();
		}

		throw new NoSuchElementException();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		throw new UnsupportedOperationException("not supported");

	}

	@Override
	public void close() throws QueryEvaluationException {
		innerIter.close();
	}

}
