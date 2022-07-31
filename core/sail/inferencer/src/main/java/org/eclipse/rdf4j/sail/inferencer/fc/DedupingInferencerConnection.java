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
package org.eclipse.rdf4j.sail.inferencer.fc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DedupingInferencerConnection extends InferencerConnectionWrapper {

	private static final int MAX_SIZE = 10000;

	private static final Logger logger = LoggerFactory.getLogger(DedupingInferencerConnection.class);

	private final ValueFactory valueFactory;

	private Set<Statement> addedStmts;

	private int dedupCount;

	public DedupingInferencerConnection(InferencerConnection con, ValueFactory vf) {
		super(con);
		this.valueFactory = vf;
		this.addedStmts = createDedupBuffer();
	}

	private Set<Statement> createDedupBuffer() {
		return Collections.newSetFromMap(new LRUMap<>(MAX_SIZE));
	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		if (contexts.length == 0) {
			// most inferred statements don't have a context so let's just deal
			// with those
			Statement stmt = valueFactory.createStatement(subj, pred, obj);
			if (addedStmts.add(stmt)) {
				return super.addInferredStatement(subj, pred, obj);
			} else {
				dedupCount++;
				return false;
			}
		} else {
			return super.addInferredStatement(subj, pred, obj, contexts);
		}
	}

	@Override
	public boolean removeInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		Statement stmt = valueFactory.createStatement(subj, pred, obj);
		addedStmts.remove(stmt);
		return super.removeInferredStatement(subj, pred, obj, contexts);
	}

	@Override
	public void clearInferred(Resource... contexts) throws SailException {
		resetDedupBuffer();
		super.clearInferred(contexts);
	}

	@Override
	public void commit() throws SailException {
		super.commit();
		logger.debug("Added {} unique statements, deduped {}", addedStmts.size(), dedupCount);
		resetDedupBuffer();
	}

	@Override
	public void rollback() throws SailException {
		super.rollback();
		resetDedupBuffer();
	}

	private void resetDedupBuffer() {
		addedStmts = null; // allow gc before alloc
		addedStmts = createDedupBuffer();
		dedupCount = 0;
	}

	static class LRUMap<K, V> extends LinkedHashMap<K, V> {

		final int maxSize;

		LRUMap(int maxSize) {
			super(maxSize, 0.75f, true);
			this.maxSize = maxSize;
		}

		@Override
		protected boolean removeEldestEntry(Entry<K, V> entry) {
			return size() > maxSize;
		}
	}
}
