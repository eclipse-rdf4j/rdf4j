/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import org.apache.commons.collections4.map.ReferenceMap;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReadCache implements DataStructureInterface {

	private static final Logger logger = LoggerFactory.getLogger(ReadCache.class);

	DataStructureInterface delegate;

	final int STATEMENTS_PER_CACHE_ITEM_LIMIT = 100000;

	// ReferenceMap defaults to hard reference for key and soft reference for value. This means that a value may be
	// garbage collected, but unlike a weak ref it will only be garbage collected if we run out of memory. This is a
	// memory sensitive cache.
	ReferenceMap<PartialStatement, List<ExtensibleStatement>> cache = new ReferenceMap<>();

	// The cache ticket is incremented every time the cache is cleared. A getStatements operation retrieves the current
	// cacheTicket when the iteration is opened. When the iteration is closed it checks the retrieved ticket against the
	// currentTicket, if they are the same then the statements can be cached. If the tickets don't match it means that
	// there was a write operation at some point after the iteration was opened, and the statements accumulated in the
	// iteration are stale and can not be cached.
	private volatile long cacheTicket = Long.MIN_VALUE;

	public ReadCache(DataStructureInterface delegate) {
		this.delegate = delegate;
	}

	@Override
	public void addStatement(ExtensibleStatement statement) {
		delegate.addStatement(statement);
		clearCache();
	}

	@Override
	public void removeStatement(ExtensibleStatement statement) {
		delegate.removeStatement(statement);
		clearCache();
	}

	@Override
	public CloseableIteration<? extends ExtensibleStatement, SailException> getStatements(Resource subject,
			IRI predicate,
			Value object, boolean inferred, Resource... context) {

		PartialStatement partialStatement = new PartialStatement(subject, predicate, object, inferred, context);
		CloseableIteration<? extends ExtensibleStatement, SailException> cached = getCached(partialStatement);
		if (cached != null) {
			logger.trace("cache hit");
			return cached;
		}

		long localCacheTicket = cacheTicket;

		return new CloseableIteration<ExtensibleStatement, SailException>() {

			CloseableIteration<? extends ExtensibleStatement, SailException> statements = delegate.getStatements(
					subject,
					predicate, object, inferred, context);
			List<ExtensibleStatement> cache = new ArrayList<>();

			@Override
			public boolean hasNext() throws SailException {
				return statements.hasNext();
			}

			@Override
			public ExtensibleStatement next() throws SailException {

				ExtensibleStatement next = statements.next();

				if (cache != null) {
					cache.add(next);
					if (cache.size() > STATEMENTS_PER_CACHE_ITEM_LIMIT) {
						cache = null;
						logger.trace("cache limit");
					}
				}

				return next;
			}

			@Override
			public void remove() throws SailException {

			}

			@Override
			public void close() throws SailException {
				if (!statements.hasNext()) {
					submitToCache(localCacheTicket, partialStatement, cache);
				} else {
					logger.trace("iteration was not fully consumed before being closed and could not be cached");
				}
				statements.close();

			}
		};

	}

	synchronized private CloseableIteration<? extends ExtensibleStatement, SailException> getCached(
			PartialStatement partialStatement) {
		List<ExtensibleStatement> statements = cache.get(partialStatement);

		if (statements != null) {

			return new LookAheadIteration<ExtensibleStatement, SailException>() {
				Iterator<ExtensibleStatement> iterator = statements.iterator();

				@Override
				protected ExtensibleStatement getNextElement() throws SailException {
					if (iterator.hasNext()) {
						return iterator.next();
					}
					return null;
				}
			};

		}

		return null;
	}

	@Override
	public void flushForReading() {
		delegate.flushForReading();
	}

	@Override
	public void init() {
		delegate.init();
	}

	@Override
	public void clear(boolean inferred, Resource[] contexts) {
		delegate.clear(inferred, contexts);
		clearCache();
	}

	@Override
	public void flushForCommit() {
		delegate.flushForCommit();
		clearCache();
	}

	@Override
	public boolean removeStatementsByQuery(Resource subj, IRI pred, Value obj, boolean inferred, Resource[] contexts) {
		boolean removed = delegate.removeStatementsByQuery(subj, pred, obj, inferred, contexts);
		clearCache();
		return removed;
	}

	synchronized public void clearCache() {
		if (!cache.isEmpty()) {
			cache.clear();
		}

		// overflow is not a problem since we use == to compare in submitToCache
		cacheTicket++;
	}

	synchronized public void submitToCache(Long localCacheTicket, PartialStatement partialStatement,
			List<ExtensibleStatement> statements) {
		if (localCacheTicket == cacheTicket && statements != null) {
			cache.put(partialStatement, statements);
		}

	}

	@Override
	public long getEstimatedSize() {
		return delegate.getEstimatedSize();
	}
}
