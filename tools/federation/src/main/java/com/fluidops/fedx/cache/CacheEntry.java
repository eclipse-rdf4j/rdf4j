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
package com.fluidops.fedx.cache;

import java.io.Serializable;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;

import com.fluidops.fedx.cache.Cache.StatementSourceAssurance;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.exception.EntryUpdateException;


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
	 * Initially it is pessimistically assumed that each endpoint can provide result. Hence,
	 * if some endpoint is not known to the cache, the cache return POSSIBLY_HAS_STATEMENTS
	 * 
	 * @param endpoint
	 * @return the {@link StatementSourceAssurance}
	 */
	public StatementSourceAssurance canProvideStatements(Endpoint endpoint);
	
	
	/**
	 * Returns a list of endpoints for which this cache result has any locally available data.
	 * 
	 * @return
	 * 		a list of endpoints or the empty list
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
	 * Return the endpoints that are mirrored in the cache result
	 * 
	 * @return the endpoints
	 */
	public List<Endpoint> getEndpoints();
	
	
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
