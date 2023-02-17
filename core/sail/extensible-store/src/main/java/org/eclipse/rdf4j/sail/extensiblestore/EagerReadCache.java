/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import java.util.Iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A read cache that keeps a hot copy of the underlying data structure
 */
public class EagerReadCache implements DataStructureInterface {

	private static final Logger logger = LoggerFactory.getLogger(EagerReadCache.class);

	DataStructureInterface delegate;

	private volatile LinkedHashModel cache = null;

	public EagerReadCache(DataStructureInterface delegate) {
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
			IRI predicate, Value object, boolean inferred, Resource... context) {
		Model cache = this.cache;

		if (cache == null) {
			if (subject != null && predicate != null && object != null) {
				// The NotifyingSail will typically trigger this method argument pattern when it checks if a statement
				// already exists before adding it. If we triggered fillCache() for that use case we would get a lot of
				// cache thrashing because the subsequent write operation would clear the cache.
				return delegate.getStatements(subject, predicate, object, inferred, context);
			}

			cache = fillCache();
		}

		Iterable<Statement> statements = cache.getStatements(subject, predicate, object, context);
		return new FilteringIteration<>(new LookAheadIteration<>() {

			final Iterator<Statement> iterator = statements.iterator();

			@Override
			protected ExtensibleStatement getNextElement() throws SailException {
				if (iterator.hasNext()) {
					Statement next = iterator.next();
					return (ExtensibleStatement) ((LinkedHashModel.ModelStatement) next).getStatement();
				}
				return null;
			}
		}, subject, predicate, object, inferred, context);
	}

	private synchronized Model fillCache() {
		LinkedHashModel cache = this.cache;
		if (cache != null) {
			return cache;
		}

		logger.debug("Filling cache");

		cache = new LinkedHashModel();

		try (var statements = delegate.getStatements(null, null, null, true)) {
			while (statements.hasNext()) {
				cache.add(statements.next());
			}
		}

		try (var statements = delegate.getStatements(null, null, null, false)) {
			while (statements.hasNext()) {
				cache.add(statements.next());
			}
		}

		this.cache = cache;

		logger.debug("Cache filled");

		return cache;
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

	public void clearCache() {
		if (cache != null) {
			logger.debug("Clearing cache");
		}
		cache = null;
	}

	@Override
	public long getEstimatedSize() {
		Model cache = this.cache;
		if (cache != null) {
			return cache.size();
		}
		return delegate.getEstimatedSize();
	}
}
