/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.repository;

import org.eclipse.rdf4j.federated.EndpointManager;
import org.eclipse.rdf4j.federated.FedX;
import org.eclipse.rdf4j.federated.FederationManager;
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

	public FedXRepository(FedX federation) {
		super(federation);
		this.federation = federation;
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

		FederationManager instance = FederationManager.getInstance();
		instance.updateStrategy();
		instance.reset();

		EndpointManager.initialize(federation.getMembers());

		super.initializeInternal();
	}

}
