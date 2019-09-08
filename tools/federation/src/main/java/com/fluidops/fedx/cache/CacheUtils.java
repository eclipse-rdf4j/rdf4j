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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import com.fluidops.fedx.algebra.StatementSource;
import com.fluidops.fedx.algebra.StatementSource.StatementSourceType;
import com.fluidops.fedx.cache.Cache.StatementSourceAssurance;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.evaluation.TripleSource;
import com.fluidops.fedx.exception.OptimizationException;
import com.fluidops.fedx.structures.SubQuery;

public class CacheUtils {

	
	/**
	 * Perform a "ASK" query for the provided statement to check if the endpoint can provide results.
	 * Update the cache with the new information.
	 * 
	 * @param cache
	 * @param endpoint
	 * @param stmt
	 * @return
	 * @throws OptimizationException
	 */
	private static boolean checkEndpointForResults(Cache cache, Endpoint endpoint, Resource subj, IRI pred, Value obj)
			throws OptimizationException
	{
		try {
			TripleSource t = endpoint.getTripleSource();
			boolean hasResults = t.hasStatements(subj, pred, obj);

			CacheEntry entry = createCacheEntry(endpoint, hasResults);
			cache.updateEntry( new SubQuery(subj, pred, obj), entry);
			
			return hasResults;
		} catch (Exception e) {
			throw new OptimizationException("Error checking results for endpoint " + endpoint.getId() + ": " + e.getMessage(), e);
		}
	}	
	
	
	public static CacheEntry createCacheEntry(Endpoint e, boolean canProvideStatements) {
		CacheEntryImpl c = new CacheEntryImpl();
		c.add( new EndpointEntry(e.getId(), canProvideStatements));
		return c;
	}
	
	
	
	/**
	 * Checks the cache if some endpoint can provide results to the subquery. If the cache has no
	 * knowledge a remote ask query is performed and the cache is updated with appropriate information.
	 * 
	 * @param cache
	 * @param endpoints
	 * @param subj
	 * @param pred
	 * @param obj
	 * @return whether some endpoint can provide results
	 */
	public static boolean checkCacheUpdateCache(Cache cache, List<Endpoint> endpoints, Resource subj, IRI pred,
			Value obj)
	{
		
		SubQuery q = new SubQuery(subj, pred, obj);
		
		for (Endpoint e : endpoints) {
			StatementSourceAssurance a = cache.canProvideStatements(q, e);
			if (a==StatementSourceAssurance.HAS_LOCAL_STATEMENTS || a==StatementSourceAssurance.HAS_REMOTE_STATEMENTS)
				return true;	
			if (a==StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS && checkEndpointForResults(cache, e, subj, pred, obj))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks the cache for relevant statement sources to the provided statement. If the cache has no
	 * knowledge ask the endpoint for further information.
	 * 
	 * @param cache
	 * @param endpoints
	 * @param subj
	 * @param pred
	 * @param obj
	 * 
	 * @return the list of relevant statement sources
	 */
	public static List<StatementSource> checkCacheForStatementSourcesUpdateCache(Cache cache, List<Endpoint> endpoints,
			Resource subj, IRI pred, Value obj)
	{
		
		SubQuery q = new SubQuery(subj, pred, obj);
		List<StatementSource> sources = new ArrayList<StatementSource>(endpoints.size());
		
		for (Endpoint e : endpoints) {
			StatementSourceAssurance a = cache.canProvideStatements(q, e);

			if (a==StatementSourceAssurance.HAS_LOCAL_STATEMENTS) {
				sources.add( new StatementSource(e.getId(), StatementSourceType.LOCAL));			
			} else if (a==StatementSourceAssurance.HAS_REMOTE_STATEMENTS) {
				sources.add( new StatementSource(e.getId(), StatementSourceType.REMOTE));			
			} else if (a==StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS) {
				
				// check if the endpoint has results (statistics + ask request)				
				if (CacheUtils.checkEndpointForResults(cache, e, subj, pred, obj))
					sources.add( new StatementSource(e.getId(), StatementSourceType.REMOTE));
			} 
		}
		return sources;
	}
}
