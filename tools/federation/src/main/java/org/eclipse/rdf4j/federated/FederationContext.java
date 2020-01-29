/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import org.eclipse.rdf4j.federated.evaluation.DelegateFederatedServiceResolver;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.monitoring.Monitoring;

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

	public FederationEvalStrategy getStrategy() {
		return manager.getStrategy();
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
}
