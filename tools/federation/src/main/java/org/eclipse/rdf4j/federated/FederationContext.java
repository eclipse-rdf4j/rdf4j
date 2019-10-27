/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

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

	public FederationContext(FederationManager manager, EndpointManager endpointManager) {
		super();
		this.manager = manager;
		this.endpointManager = endpointManager;
	}

	// TODO adjust lifecycle to have this available in the constructor
	void setManager(FederationManager manager) {
		this.manager = manager;
	}

	public FederationManager getManager() {
		return this.manager;
	}

	public EndpointManager getEndpointManager() {
		return this.endpointManager;
	}

	public FederationEvalStrategy getStrategy() {
		return manager.getStrategy();
	}

	public Monitoring getMonitoringService() {
		return manager.getMonitoring();
	}
}
