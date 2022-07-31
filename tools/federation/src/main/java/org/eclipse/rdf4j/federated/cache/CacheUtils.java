/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.cache;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.StatementSource.StatementSourceType;
import org.eclipse.rdf4j.federated.cache.SourceSelectionCache.StatementSourceAssurance;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
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
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param queryInfo
	 * @param contexts
	 * @return
	 * @throws OptimizationException
	 */
	private static boolean checkEndpointForResults(SourceSelectionCache cache, Endpoint endpoint, Resource subj,
			IRI pred, Value obj, QueryInfo queryInfo, Resource... contexts)
			throws OptimizationException {
		try {
			TripleSource t = endpoint.getTripleSource();
			boolean hasResults = t.hasStatements(subj, pred, obj, queryInfo, contexts);

			cache.updateInformation(new SubQuery(subj, pred, obj, contexts), endpoint, hasResults);

			return hasResults;
		} catch (Exception e) {
			throw new OptimizationException(
					"Error checking results for endpoint " + endpoint.getId() + ": " + e.getMessage(), e);
		}
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
	 * @param queryInfo
	 * @param contexts
	 * @return whether some endpoint can provide results
	 */
	public static boolean checkCacheUpdateCache(SourceSelectionCache cache, List<Endpoint> endpoints, Resource subj,
			IRI pred,
			Value obj, QueryInfo queryInfo, Resource... contexts) {

		SubQuery q = new SubQuery(subj, pred, obj, contexts);

		for (Endpoint e : endpoints) {
			StatementSourceAssurance a = cache.getAssurance(q, e);
			if (a == StatementSourceAssurance.HAS_REMOTE_STATEMENTS) {
				return true;
			}
			if (a == StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS
					&& checkEndpointForResults(cache, e, subj, pred, obj, queryInfo, contexts)) {
				return true;
			}
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
	 * @param queryInfo
	 * @param contexts
	 *
	 * @return the list of relevant statement sources
	 */
	public static List<StatementSource> checkCacheForStatementSourcesUpdateCache(SourceSelectionCache cache,
			List<Endpoint> endpoints,
			Resource subj, IRI pred, Value obj, QueryInfo queryInfo, Resource... contexts) {

		SubQuery q = new SubQuery(subj, pred, obj, contexts);
		List<StatementSource> sources = new ArrayList<>(endpoints.size());

		for (Endpoint e : endpoints) {
			StatementSourceAssurance a = cache.getAssurance(q, e);

			if (a == StatementSourceAssurance.HAS_REMOTE_STATEMENTS) {
				sources.add(new StatementSource(e.getId(), StatementSourceType.REMOTE));
			} else if (a == StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS) {

				// check if the endpoint has results (statistics + ask request)
				if (CacheUtils.checkEndpointForResults(cache, e, subj, pred, obj, queryInfo, contexts)) {
					sources.add(new StatementSource(e.getId(), StatementSourceType.REMOTE));
				}
			}
		}
		return sources;
	}
}
