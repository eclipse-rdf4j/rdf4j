/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * A cache for MemStatementIterator that tracks how frequently an iterator is used and caches the iterator as a list
 *
 * @author Håvard M. Ottestad
 */
public class MemStatementIteratorCache {

	private final static Logger logger = LoggerFactory.getLogger(MemStatementIteratorCache.class);

	// the number of times an iterator needs to be used before it will be cached
	public final int CACHE_FREQUENCY_THRESHOLD;

	// a map that tracks the number of times a cacheable iterator has been used
	private final ConcurrentHashMap<MemStatementIterator, Integer> iteratorFrequencyMap = new ConcurrentHashMap<>();

	// a cache for commonly used iterators that are particularly costly
	private final Cache<MemStatementIterator, List<MemStatement>> iteratorCache = CacheBuilder
			.newBuilder()
			.softValues()
			.build();

	public MemStatementIteratorCache(int cacheFrequencyThreshold) {
		this.CACHE_FREQUENCY_THRESHOLD = cacheFrequencyThreshold;
	}

	public void invalidateCache() {
		if (!(iteratorFrequencyMap.isEmpty())) {
			iteratorFrequencyMap.clear();
			iteratorCache.invalidateAll();
			iteratorCache.cleanUp();

			if (logger.isTraceEnabled()) {
				logger.debug("Invalidated cache", new Throwable());
			} else if (logger.isDebugEnabled()) {
				logger.debug("Invalidated cache");
			}
		}
	}

	void incrementIteratorFrequencyMap(MemStatementIterator iterator) {
		Integer compute = iteratorFrequencyMap.compute(iterator, (key, value) -> {
			if (value == null) {
				return 0;
			}
			return value + 1;
		});
		if (logger.isDebugEnabled()) {
			logger.debug("Incremented iteratorFrequencyMap to {}\n{} \n{}", compute, iterator, iterator.getStats());
		}
	}

	boolean shouldBeCached(MemStatementIterator iterator) {
		if (!iteratorFrequencyMap.isEmpty()) {
			Integer integer = iteratorFrequencyMap.get(iterator);
			return integer != null && integer > CACHE_FREQUENCY_THRESHOLD;
		} else {
			return false;
		}
	}

	CachedIteration getCachedIterator(MemStatementIterator iterator) {

		List<MemStatement> cached = iteratorCache.getIfPresent(iterator);

		if (cached == null) {
			try (iterator) {
				logger.debug("Filling cache {}", iterator);
				ArrayList<MemStatement> newCache = new ArrayList<>();
				while (iterator.hasNext()) {
					newCache.add(iterator.next());
				}
				newCache.trimToSize();
				cached = Collections.unmodifiableList(newCache);
			}
			iteratorCache.put(iterator, cached);
		}

		return new CachedIteration(cached.iterator());
	}

	private static class CachedIteration implements CloseableIteration<MemStatement, SailException> {

		private Iterator<MemStatement> iter;

		public CachedIteration(Iterator<MemStatement> iter) {
			this.iter = iter;
		}

		@Override
		public boolean hasNext() throws SailException {
			if (iter == null) {
				return false;
			}

			boolean result = iter.hasNext();
			if (!result) {
				close();
			}
			return result;
		}

		@Override
		public MemStatement next() throws SailException {
			if (iter == null) {
				throw new NoSuchElementException("Iteration has been closed");
			}

			return iter.next();
		}

		@Override
		public void remove() throws SailException {
			if (iter == null) {
				throw new IllegalStateException("Iteration has been closed");
			}

			iter.remove();
		}

		@Override
		public final void close() throws SailException {
			iter = null;
		}

	}

}
