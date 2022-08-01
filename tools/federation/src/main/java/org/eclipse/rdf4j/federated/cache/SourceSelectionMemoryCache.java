/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.cache;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.structures.SubQuery;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.Maps;

/**
 * An implementation of {@link SourceSelectionCache} which uses an in memory Guava {@link Cache} as data structure to
 * maintain information.
 *
 * @author Andreas Schwarte
 *
 */
public class SourceSelectionMemoryCache implements SourceSelectionCache {

	public static final String DEFAULT_CACHE_SPEC = "maximumSize=1000,expireAfterWrite=6h";

	private final Cache<SubQuery, Entry> cache;

	public SourceSelectionMemoryCache() {
		this(DEFAULT_CACHE_SPEC);
	}

	/**
	 *
	 * @param cacheSpec a Guava compatible {@link CacheBuilderSpec}, if <code>null</code> the
	 *                  {@link #DEFAULT_CACHE_SPEC} is used
	 */
	public SourceSelectionMemoryCache(String cacheSpec) {
		cacheSpec = cacheSpec == null ? DEFAULT_CACHE_SPEC : cacheSpec;
		this.cache = CacheBuilder.from(CacheBuilderSpec.parse(cacheSpec)).build();
	}

	@Override
	public StatementSourceAssurance getAssurance(SubQuery subQuery, Endpoint endpoint) {

		// for patterns with three variables we always check the endpoint
		if (subQuery.isUnbound()) {
			return StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS;
		}

		Entry entry = cache.getIfPresent(subQuery);
		if (entry != null) {
			return entry.getAssurance(endpoint);
		}

		// check if we can infer something from other cached entries
		// if endpoint does not have data for {?s foaf:name ?o}, it does also not have data for {?s foaf:name "Alan" }
		if (subQuery.object() != null) {
			if (getAssurance(new SubQuery(subQuery.subject(), subQuery.predicate(), null, subQuery.contexts()),
					endpoint)
							.equals(StatementSourceAssurance.NONE)) {
				return StatementSourceAssurance.NONE;
			}
		}

		// no information in the cache, we have to check at the source
		return StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS;
	}

	@Override
	public void updateInformation(SubQuery subQuery, Endpoint endpoint, boolean hasStatements) {

		updateCacheEntry(subQuery, endpoint, hasStatements);
		updateInferredInformation(subQuery, endpoint, hasStatements);
	}

	private void updateCacheEntry(SubQuery subQuery, Endpoint endpoint, boolean hasStatements) {
		Entry entry;
		try {
			entry = cache.get(subQuery, () -> new Entry());
			entry.setEndpointInfo(endpoint, hasStatements);
		} catch (ExecutionException e) {
			throw new FedXRuntimeException(e);
		}
	}

	private void updateInferredInformation(SubQuery subQuery, Endpoint endpoint, boolean hasStatements) {

		if (!hasStatements) {
			return; // we cannot say for sure in this case
		}
		if (subQuery.object() != null) {

			if (subQuery.predicate() != null) {
				updateCacheEntry(new SubQuery(subQuery.subject(), subQuery.predicate(), null), endpoint, hasStatements);
			}
		}
	}

	/**
	 * Entry representing the state for a sub query
	 *
	 * @author Andreas Schwarte
	 *
	 */
	private static class Entry {
		final Map<String, StatementSourceAssurance> endpointToInformation = Maps.newConcurrentMap();

		public void setEndpointInfo(Endpoint e, boolean hasStatements) {
			endpointToInformation.put(e.getId(),
					hasStatements ? StatementSourceAssurance.HAS_REMOTE_STATEMENTS : StatementSourceAssurance.NONE);
		}

		/**
		 * The {@link StatementSourceAssurance} for the given {@link Endpoint},
		 * {@link StatementSourceAssurance#POSSIBLY_HAS_STATEMENTS if unknown}.
		 *
		 * @param e
		 */
		public StatementSourceAssurance getAssurance(Endpoint e) {
			return endpointToInformation.getOrDefault(e.getId(), StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS);
		}
	}

}
