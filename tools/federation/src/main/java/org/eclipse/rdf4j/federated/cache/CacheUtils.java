/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.cache;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.StatementSource.StatementSourceType;
import org.eclipse.rdf4j.federated.cache.Cache.StatementSourceAssurance;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.federated.structures.SubQuery;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

public class CacheUtils {

	/**
	 * Perform a "ASK" query for the provided statement to check if the endpoint can provide results. Update the cache
	 * with the new information.
	 * 
	 * @param cache
	 * @param endpoint
	 * @param stmt
	 * @return
	 * @throws OptimizationException
	 */
	private static boolean checkEndpointForResults(Cache cache, Endpoint endpoint, Resource subj, IRI pred, Value obj)
			throws OptimizationException {
		try {
			TripleSource t = endpoint.getTripleSource();
			boolean hasResults = t.hasStatements(subj, pred, obj);

			CacheEntry entry = createCacheEntry(endpoint, hasResults);
			cache.updateEntry(new SubQuery(subj, pred, obj), entry);

			return hasResults;
		} catch (Exception e) {
			throw new OptimizationException(
					"Error checking results for endpoint " + endpoint.getId() + ": " + e.getMessage(), e);
		}
	}

	public static CacheEntry createCacheEntry(Endpoint e, boolean canProvideStatements) {
		CacheEntryImpl c = new CacheEntryImpl();
		c.add(new EndpointEntry(e.getId(), canProvideStatements));
		return c;
	}

	/**
	 * Checks the cache if some endpoint can provide results to the subquery. If the cache has no knowledge a remote ask
	 * query is performed and the cache is updated with appropriate information.
	 * 
	 * @param cache
	 * @param endpoints
	 * @param subj
	 * @param pred
	 * @param obj
	 * @return whether some endpoint can provide results
	 */
	public static boolean checkCacheUpdateCache(Cache cache, List<Endpoint> endpoints, Resource subj, IRI pred,
			Value obj) {

		SubQuery q = new SubQuery(subj, pred, obj);

		for (Endpoint e : endpoints) {
			StatementSourceAssurance a = cache.canProvideStatements(q, e);
			if (a == StatementSourceAssurance.HAS_LOCAL_STATEMENTS
					|| a == StatementSourceAssurance.HAS_REMOTE_STATEMENTS)
				return true;
			if (a == StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS
					&& checkEndpointForResults(cache, e, subj, pred, obj))
				return true;
		}
		return false;
	}

	/**
	 * Checks the cache for relevant statement sources to the provided statement. If the cache has no knowledge ask the
	 * endpoint for further information.
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
			Resource subj, IRI pred, Value obj) {

		SubQuery q = new SubQuery(subj, pred, obj);
		List<StatementSource> sources = new ArrayList<>(endpoints.size());

		for (Endpoint e : endpoints) {
			StatementSourceAssurance a = cache.canProvideStatements(q, e);

			if (a == StatementSourceAssurance.HAS_LOCAL_STATEMENTS) {
				sources.add(new StatementSource(e.getId(), StatementSourceType.LOCAL));
			} else if (a == StatementSourceAssurance.HAS_REMOTE_STATEMENTS) {
				sources.add(new StatementSource(e.getId(), StatementSourceType.REMOTE));
			} else if (a == StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS) {

				// check if the endpoint has results (statistics + ask request)
				if (CacheUtils.checkEndpointForResults(cache, e, subj, pred, obj))
					sources.add(new StatementSource(e.getId(), StatementSourceType.REMOTE));
			}
		}
		return sources;
	}
}
