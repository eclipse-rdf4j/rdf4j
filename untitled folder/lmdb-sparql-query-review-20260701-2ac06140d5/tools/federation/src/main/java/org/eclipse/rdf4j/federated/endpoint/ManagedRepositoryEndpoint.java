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
package org.eclipse.rdf4j.federated.endpoint;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.endpoint.provider.RepositoryInformation;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * A specialized {@link RepositoryEndpoint} where the lifecycle of the {@link Repository} is managed by this endpoint,
 * i.e. this instance takes care for initialize and shutdown of the repository.
 *
 * @author Andreas Schwarte
 * @see RepositoryEndpoint
 */
public class ManagedRepositoryEndpoint extends RepositoryEndpoint {

	public ManagedRepositoryEndpoint(RepositoryInformation repoInfo, String endpoint,
			EndpointClassification endpointClassification, Repository repo) {
		super(repoInfo, endpoint, endpointClassification, repo);
	}

	@Override
	public void init(FederationContext federationContext) throws RepositoryException {
		if (isInitialized()) {
			return;
		}
		repository.init();
		super.init(federationContext);
	}

	@Override
	public void shutDown() throws RepositoryException {
		try {
			super.shutDown();
		} finally {
			repository.shutDown();
		}
	}
}
