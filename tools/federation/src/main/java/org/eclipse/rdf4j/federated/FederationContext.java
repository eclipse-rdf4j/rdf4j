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
package org.eclipse.rdf4j.federated;

import org.eclipse.rdf4j.federated.cache.SourceSelectionCache;
import org.eclipse.rdf4j.federated.cache.SourceSelectionMemoryCache;
import org.eclipse.rdf4j.federated.evaluation.DelegateFederatedServiceResolver;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.monitoring.Monitoring;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

/**
 * Context to maintain the state of the current federation
 *
 * @author Andreas Schwarte
 *
 */
public class FederationContext {

	private final FederationManager manager;

	private final EndpointManager endpointManager;

	private final Monitoring monitoring;

	private final QueryManager queryManager;

	private final DelegateFederatedServiceResolver serviceResolver;

	private final FedXConfig fedXConfig;

	private final SourceSelectionCache sourceSelectionCache;

	public FederationContext(FederationManager manager, EndpointManager endpointManager, QueryManager queryManager,
			DelegateFederatedServiceResolver federatedServiceResolver,
			Monitoring monitoring, FedXConfig fedXConfig) {
		super();
		this.manager = manager;
		this.endpointManager = endpointManager;
		this.queryManager = queryManager;
		this.serviceResolver = federatedServiceResolver;
		this.monitoring = monitoring;
		this.fedXConfig = fedXConfig;
		this.sourceSelectionCache = createSourceSelectionCache();
	}

	public FedX getFederation() {
		return this.manager.getFederation();
	}

	public FederationManager getManager() {
		return this.manager;
	}

	public QueryManager getQueryManager() {
		return this.queryManager;
	}

	public EndpointManager getEndpointManager() {
		return this.endpointManager;
	}

	public Monitoring getMonitoringService() {
		return this.monitoring;
	}

	public DelegateFederatedServiceResolver getFederatedServiceResolver() {
		return this.serviceResolver;
	}

	public FedXConfig getConfig() {
		return this.fedXConfig;
	}

	public SourceSelectionCache getSourceSelectionCache() {
		return this.sourceSelectionCache;
	}

	/**
	 * Create a fresh {@link FederationEvalStrategy} using information from this federation context.
	 */
	public FederationEvalStrategy createStrategy(Dataset dataset) {
		TripleSource tripleSource = null;
		EvaluationStatistics evaluationStatistics = null;
		return manager.getFederationEvaluationStrategyFactory()
				.createEvaluationStrategy(dataset, tripleSource, evaluationStatistics);
	}

	/**
	 * Create the {@link SourceSelectionCache}
	 *
	 * @return the {@link SourceSelectionCache}
	 * @see FedXConfig#getSourceSelectionCacheSpec()
	 */
	private SourceSelectionCache createSourceSelectionCache() {
		String cacheSpec = getConfig().getSourceSelectionCacheSpec();
		return new SourceSelectionMemoryCache(cacheSpec);
	}
}
