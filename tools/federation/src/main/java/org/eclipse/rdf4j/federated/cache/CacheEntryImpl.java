/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.cache.Cache.StatementSourceAssurance;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.exception.EntryUpdateException;
import org.eclipse.rdf4j.model.Statement;

/**
 * Implementation for Cache Entry
 * 
 * @author Andreas Schwarte
 *
 */
public class CacheEntryImpl implements CacheEntry {

	private static final long serialVersionUID = -2078321733800349639L;

	/* map endpoint.id to the corresponding entry */
	protected Map<String, EndpointEntry> entries = new HashMap<>();

	@Override
	public StatementSourceAssurance canProvideStatements(Endpoint endpoint) {
		EndpointEntry entry = entries.get(endpoint.getId());
		return entry == null ? StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS
				: (entry.doesProvideStatements() ? StatementSourceAssurance.HAS_REMOTE_STATEMENTS
						: StatementSourceAssurance.NONE);
	}

	@Override
	public CloseableIteration<? extends Statement, Exception> getStatements() {
		throw new UnsupportedOperationException("This operation is not yet supported.");
	}

	@Override
	public CloseableIteration<? extends Statement, Exception> getStatements(
			Endpoint endpoint) {
		throw new UnsupportedOperationException("This operation is not yet supported.");
	}

	@Override
	public List<Endpoint> hasLocalStatements() {
		throw new UnsupportedOperationException("This operation is not yet supported.");
	}

	@Override
	public boolean hasLocalStatements(Endpoint endpoint) {
		// not yet implemented
		return false;
	}

	@Override
	public boolean isUpToDate() {
		return true;
	}

	@Override
	public void merge(CacheEntry other) throws EntryUpdateException {
		// XXX make a check if we can safely cast?

		CacheEntryImpl o = (CacheEntryImpl) other;

		for (String k : o.entries.keySet()) {
			if (!entries.containsKey(k))
				entries.put(k, o.entries.get(k));
			else {

				EndpointEntry _merge = o.entries.get(k);
				EndpointEntry _old = entries.get(k);

				_old.setCanProvideStatements(_merge.doesProvideStatements());
			}

		}
	}

	@Override
	public void update() throws EntryUpdateException {
		throw new UnsupportedOperationException("This operation is not yet supported.");
	}

	@Override
	public void add(EndpointEntry endpointEntry) {
		entries.put(endpointEntry.getEndpointID(), endpointEntry);
	}
}
