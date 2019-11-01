/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import org.eclipse.rdf4j.federated.cache.Cache;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.monitoring.Monitoring;

/**
 * Context to maintain the state of the current federation
 * 
 * @author Andreas Schwarte
 *
 */
public class FederationContext {

	private FederationManager manager;

	private final EndpointManager endpointManager;

	private final Monitoring monitoring;

	private QueryManager queryManager;

	private final Cache cache;

	public FederationContext(FederationManager manager, EndpointManager endpointManager, Cache cache,
			Monitoring monitoring) {
		super();
		this.manager = manager;
		this.endpointManager = endpointManager;
		this.cache = cache;
		this.monitoring = monitoring;
	}

	// TODO adjust lifecycle to have this available in the constructor
	void setManager(FederationManager manager) {
		this.manager = manager;
	}

	void setQueryManager(QueryManager queryManager) {
		this.queryManager = queryManager;
	}

	public FedX getFederation() {
		return this.getManager().federation;
	}

	public Cache getCache() {
		return this.cache;
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
}
