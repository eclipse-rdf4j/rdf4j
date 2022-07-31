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
package org.eclipse.rdf4j.federated.repository;

import org.eclipse.rdf4j.federated.EndpointManager;
import org.eclipse.rdf4j.federated.FedX;
import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.FederationManager;
import org.eclipse.rdf4j.federated.QueryManager;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointType;
import org.eclipse.rdf4j.federated.evaluation.DelegateFederatedServiceResolver;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.monitoring.Monitoring;
import org.eclipse.rdf4j.federated.monitoring.MonitoringFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A special {@link SailRepository} which performs the actions as defined in {@link FedXRepositoryConnection}.
 *
 * @author as
 */
public class FedXRepository extends SailRepository {

	private static final Logger log = LoggerFactory.getLogger(FedXRepository.class);

	private final FedX federation;

	private final FedXConfig fedXConfig;

	private FederationContext federationContext;

	/**
	 * The external {@link FederatedServiceResolver}, if any
	 */
	private FederatedServiceResolver serviceResolver = null;

	public FedXRepository(FedX federation, FedXConfig config) {
		super(federation);
		this.federation = federation;
		this.fedXConfig = config;
	}

	@Override
	public FedXRepositoryConnection getConnection() throws RepositoryException {
		if (!isInitialized()) {
			init();
		}
		try {
			return new FedXRepositoryConnection(this, this.getSail().getConnection());
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	protected void initializeInternal() throws RepositoryException {

		log.info("Initializing federation ...");

		Monitoring monitoring = MonitoringFactory.createMonitoring(fedXConfig);

		EndpointManager endpointManager = EndpointManager.initialize(federation.getMembers());

		FederationManager federationManager = new FederationManager();

		QueryManager queryManager = new QueryManager();

		DelegateFederatedServiceResolver fedxServiceResolver = new DelegateFederatedServiceResolver(
				endpointManager);
		if (serviceResolver != null) {
			fedxServiceResolver.setDelegate(serviceResolver);
		}

		federationContext = new FederationContext(federationManager, endpointManager, queryManager,
				fedxServiceResolver, monitoring, fedXConfig);
		federation.setFederationContext(federationContext);

		federationManager.init(federation, federationContext);

		super.initializeInternal();

		queryManager.init(this, federationContext);
		fedxServiceResolver.initialize();
	}

	@Override
	protected void shutDownInternal() throws RepositoryException {
		try {
			federationContext.getManager().shutDown();
		} catch (FedXException e) {
			throw new SailException(e);
		}
		super.shutDownInternal();
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
		super.setFederatedServiceResolver(resolver);

	}

	/**
	 * return the number of triples in the federation as string. Retrieving the size is only supported
	 * {@link EndpointType#NativeStore} and {@link EndpointType#RemoteRepository}.
	 *
	 * If the federation contains other types of endpoints, the size is indicated as a lower bound, i.e. the string
	 * starts with a larger sign.
	 *
	 * @return the number of triples in the federation
	 */
	public String getFederationSize() {
		long size = 0;
		boolean isLowerBound = false;
		for (Endpoint e : federation.getMembers()) {
			try {
				size += e.size();
			} catch (RepositoryException e1) {
				isLowerBound = true;
			}
		}
		return isLowerBound ? ">" + size : Long.toString(size);
	}

	/**
	 *
	 * @return the {@link FederationContext}
	 */
	public FederationContext getFederationContext() {
		return this.federationContext;
	}

	/**
	 *
	 * @return the {@link QueryManager} from the {@link FederationContext}
	 */
	public QueryManager getQueryManager() {
		return federationContext.getQueryManager();
	}

}
