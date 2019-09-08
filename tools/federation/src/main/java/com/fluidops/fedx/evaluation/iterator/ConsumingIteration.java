/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.evaluation.iterator;

import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import com.google.common.collect.Lists;


/**
 * A specialized {@link CloseableIteration} that consumes part (or the entire input
 * iteration if it fits into the buffer) and keeps data for further processing
 * in memory. If the buffer is full, the remaining items will be read from the
 * iteration lazily.
 * 
 * This implementation can be used to avoid blocking behavior in HTTP connection
 * streams, i.e. to process results in memory and close the underlying HTTP stream.
 * 
 * @author Andreas Schwarte
 *
 */
public class ConsumingIteration implements CloseableIteration<BindingSet, QueryEvaluationException> {

	
	/**
	 * Maximum number of bindings that are consumed at construction time.
	 * Remaining items, if any are consumed from the iterator itself
	 */
	private static final int max = 1000;	// TODO make configurable
	
	
	private final List<BindingSet> consumed = Lists.newArrayList();
	
	private final CloseableIteration<BindingSet, QueryEvaluationException> innerIter;

	
	/**
	 * The index of the next element that will be returned by a call to {@link #next()}.
	 */
	private int currentIndex = 0;
	
	
	public ConsumingIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter) throws QueryEvaluationException {
		
		innerIter = iter;
				
		while (consumed.size() < max && iter.hasNext()) {
			consumed.add(iter.next());
		}
		
		if (!iter.hasNext()) {
			iter.close();
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
		Iterations.closeCloseable(innerIter);
	}

}
