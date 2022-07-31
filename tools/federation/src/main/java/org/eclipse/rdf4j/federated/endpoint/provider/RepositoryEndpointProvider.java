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
package org.eclipse.rdf4j.federated.endpoint.provider;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointBase;
import org.eclipse.rdf4j.federated.endpoint.EndpointClassification;
import org.eclipse.rdf4j.federated.endpoint.ManagedRepositoryEndpoint;
import org.eclipse.rdf4j.federated.endpoint.RepositoryEndpoint;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * Returns an {@link Endpoint} for an already configured {@link Repository}.
 *
 * <p>
 * If the repository is already initialized, it is assumed that the lifecycle is managed externally (see
 * {@link RepositoryEndpoint}. Otherwise, FedX will make sure to take care for the lifecycle of the repository, i.e.
 * initialize and shutdown (see {@link ManagedRepositoryEndpoint}).
 * </p>
 *
 * @author Andreas Schwarte
 * @see RepositoryEndpoint
 * @see ManagedRepositoryEndpoint
 */
public class RepositoryEndpointProvider implements EndpointProvider<RepositoryInformation> {

	protected final Repository repository;

	public RepositoryEndpointProvider(Repository repository) {
		super();
		this.repository = repository;
	}

	@Override
	public Endpoint loadEndpoint(RepositoryInformation repoInfo)
			throws FedXException {

		try {
			boolean didInitialize = false;
			try {
				if (!repository.isInitialized()) {
					repository.init();
					didInitialize = true;
				}
			} finally {
				if (didInitialize) {
					repository.shutDown();
				}
			}

			EndpointBase res;

			if (repository.isInitialized()) {
				res = new RepositoryEndpoint(repoInfo, repoInfo.getLocation(), EndpointClassification.Remote,
						repository);
			} else {
				res = new ManagedRepositoryEndpoint(repoInfo, repoInfo.getLocation(), EndpointClassification.Remote,
						repository);
			}
			res.setEndpointConfiguration(repoInfo.getEndpointConfiguration());

			return res;
		} catch (RepositoryException e) {
			throw new FedXException("Repository " + repoInfo.getId() + " could not be initialized: " + e.getMessage(),
					e);
		}
	}
}
