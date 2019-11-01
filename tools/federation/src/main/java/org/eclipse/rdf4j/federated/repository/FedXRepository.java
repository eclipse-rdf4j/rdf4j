/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.repository;

import org.eclipse.rdf4j.federated.FedX;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.FederationManager;
import org.eclipse.rdf4j.federated.QueryManager;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointType;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A special {@link SailRepository} which performs the actions as defined in {@link FedXRepositoryConnection}.
 * 
 * @author as
 */
public class FedXRepository extends SailRepository {

	private final FedX federation;
	private final FederationContext federationContext;

	public FedXRepository(FedX federation, FederationContext federationContext) {
		super(federation);
		this.federation = federation;
		this.federationContext = federationContext;
	}

	@Override
	public FedXRepositoryConnection getConnection() throws RepositoryException {
		try {
			return new FedXRepositoryConnection(this, this.getSail().getConnection());
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	protected void initializeInternal() throws RepositoryException {

		FederationManager instance = federationContext.getManager();
		instance.updateStrategy();
		instance.reset();

		super.initializeInternal();
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
		for (Endpoint e : federation.getMembers())
			try {
				size += e.size();
			} catch (RepositoryException e1) {
				isLowerBound = true;
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
