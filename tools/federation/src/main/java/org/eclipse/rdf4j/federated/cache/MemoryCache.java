/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.exception.EntryAlreadyExistsException;
import org.eclipse.rdf4j.federated.exception.EntryUpdateException;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.structures.SubQuery;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a Main memory cache which is persisted to the provided cache location.
 * 
 * Currently only binary provenance information is maintained.
 * 
 * @author Andreas Schwarte
 *
 */
public class MemoryCache implements Cache {

	private static final Logger log = LoggerFactory.getLogger(MemoryCache.class);

	protected HashMap<SubQuery, CacheEntry> cache = new HashMap<>();
	protected File cacheLocation;

	public MemoryCache(File cacheLocation) {
		if (cacheLocation == null)
			throw new FedXRuntimeException("The provided cacheLocation must not be null.");
		this.cacheLocation = cacheLocation;
	}

	@Override
	public void addEntry(SubQuery subQuery, CacheEntry cacheEntry) throws EntryAlreadyExistsException {

		synchronized (cache) {

			if (cache.containsKey(subQuery))
				throw new EntryAlreadyExistsException("Entry for statement " + subQuery
						+ " already exists in cache. Use update functionality instead.");

			cache.put(subQuery, cacheEntry);
		}

	}

	@Override
	public void updateEntry(SubQuery subQuery, CacheEntry merge) throws EntryUpdateException {

		synchronized (cache) {

			CacheEntry entry = cache.get(subQuery);

			if (entry == null)
				cache.put(subQuery, merge);
			else
				entry.merge(merge);
		}

	}

	@Override
	public void removeEntry(SubQuery subQuery) throws EntryUpdateException {

		synchronized (cache) {
			cache.remove(subQuery);
		}

	}

	@Override
	public StatementSourceAssurance canProvideStatements(SubQuery subQuery, Endpoint endpoint) {
		CacheEntry entry = cache.get(subQuery);
		if (entry == null)
			return StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS;
		if (entry.hasLocalStatements(endpoint))
			return StatementSourceAssurance.HAS_LOCAL_STATEMENTS;
		return entry.canProvideStatements(endpoint);
	}

	@Override
	public CacheEntry getCacheEntry(SubQuery subQuery) {
		CacheEntry entry = cache.get(subQuery);
		// TODO use clone or some copy/wrapping method to have read only capability
		return entry;
	}

	@Override
	public CloseableIteration<? extends Statement, Exception> getStatements(
			SubQuery subQuery) {
		CacheEntry entry = cache.get(subQuery);
		return entry == null ? new EmptyIteration<>() : entry.getStatements();
	}

	@Override
	public CloseableIteration<? extends Statement, Exception> getStatements(
			SubQuery subQuery, Endpoint endpoint) {
		CacheEntry entry = cache.get(subQuery);
		return entry == null ? new EmptyIteration<>() : entry.getStatements(endpoint);
	}

	@Override
	public List<Endpoint> hasLocalStatements(SubQuery subQuery) {
		CacheEntry entry = cache.get(subQuery);
		return entry == null ? Collections.<Endpoint>emptyList() : entry.hasLocalStatements();
	}

	@Override
	public boolean hasLocalStatements(SubQuery subQuery, Endpoint endpoint) {
		CacheEntry entry = cache.get(subQuery);
		return entry == null ? false : entry.hasLocalStatements(endpoint);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize() throws FedXException {

		if (!cacheLocation.exists())
			return;
		try (ObjectInputStream in = new ObjectInputStream(
				new BufferedInputStream(new FileInputStream(cacheLocation)))) {
			cache = (HashMap<SubQuery, CacheEntry>) in.readObject();
		} catch (Exception e) {
			throw new FedXException("Error initializing cache.", e);
		}
	}

	@Override
	public void invalidate() throws FedXException {
		cache.clear();
	}

	@Override
	public void persist() throws FedXException {

		// XXX write to a temporary file first, to prevent a corrupt file

		try (ObjectOutputStream out = new ObjectOutputStream(
				new BufferedOutputStream(new FileOutputStream(cacheLocation)))) {
			out.writeObject(this.cache);
		} catch (Exception e) {
			throw new FedXException("Error persisting cache data.", e);
		}
	}

	@Override
	public void clear() {
		log.debug("Clearing the cache.");
		cache = new HashMap<>();

	}
}
