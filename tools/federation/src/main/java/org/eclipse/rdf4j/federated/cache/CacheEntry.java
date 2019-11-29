/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.cache;

import java.io.Serializable;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.cache.Cache.StatementSourceAssurance;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.exception.EntryUpdateException;
import org.eclipse.rdf4j.model.Statement;

/**
 * Interface for a CacheEntry
 * 
 * @author Andreas Schwarte
 *
 */
public interface CacheEntry extends Serializable {

	/**
	 * Returns HAS_STATEMENTS, if this endpoint can provide results, NONE in the contrary case.
	 * 
	 * Initially it is pessimistically assumed that each endpoint can provide result. Hence, if some endpoint is not
	 * known to the cache, the cache return POSSIBLY_HAS_STATEMENTS
	 * 
	 * @param endpoint
	 * @return the {@link StatementSourceAssurance}
	 */
	public StatementSourceAssurance canProvideStatements(Endpoint endpoint);

	/**
	 * Returns a list of endpoints for which this cache result has any locally available data.
	 * 
	 * @return a list of endpoints or the empty list
	 */
	public List<Endpoint> hasLocalStatements();

	/**
	 * Returns true iff this cache result has any local available data for the specified endpoint.
	 * 
	 * @param endpoint
	 * @return whether local statements are available
	 */
	public boolean hasLocalStatements(Endpoint endpoint);

	/**
	 * Return all results available for this cache entry.
	 * 
	 * @return the statements of this entry
	 */
	public CloseableIteration<? extends Statement, Exception> getStatements();

	/**
	 * Return all results available for the specified endpoint.
	 * 
	 * @param endpoint
	 * @return the statements for the given endpoint
	 */
	public CloseableIteration<? extends Statement, Exception> getStatements(Endpoint endpoint);

	/**
	 * Ask if this CacheResult is up2date
	 * 
	 * @return whether this entry is up to date
	 */
	public boolean isUpToDate();

	/**
	 * Update this cache result, e.g. retrieve contents from web source (Optional Operation)
	 * 
	 * @throws EntryUpdateException
	 */
	public void update() throws EntryUpdateException;

	public void add(EndpointEntry endpointEntry);

	/**
	 * Update this cache entry and merge data with the specified item.
	 * 
	 * @param other
	 * @throws EntryUpdateException
	 */
	public void merge(CacheEntry other) throws EntryUpdateException;
}
