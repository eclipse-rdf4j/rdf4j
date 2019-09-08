/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx.endpoint.provider;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.endpoint.EndpointBase;
import com.fluidops.fedx.endpoint.EndpointClassification;
import com.fluidops.fedx.endpoint.ManagedRepositoryEndpoint;
import com.fluidops.fedx.endpoint.RepositoryEndpoint;
import com.fluidops.fedx.exception.FedXException;

/**
 * Returns an {@link Endpoint} for an already configured {@link Repository}.
 * 
 * <p>
 * If the repository is already initialized, it is assumed that the lifecycle is
 * managed externally (see {@link RepositoryEndpoint}. Otherwise, FedX will make
 * sure to take care for the lifecycle of the repository, i.e. initialize and
 * shutdown (see {@link ManagedRepositoryEndpoint}).
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
					repository.initialize();
					didInitialize = true;
				}

				ProviderUtil.checkConnectionIfConfigured(repository);
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
