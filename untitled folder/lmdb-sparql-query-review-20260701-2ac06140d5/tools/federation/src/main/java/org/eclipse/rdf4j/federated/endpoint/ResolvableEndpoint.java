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

import org.eclipse.rdf4j.federated.endpoint.provider.RepositoryInformation;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryResolver;

/**
 * A specialized {@link Endpoint} that allows to resolve the repository using a {@link RepositoryResolver}.
 * <p>
 * The Repository lifecycle is assumed to be managed externally.
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public class ResolvableEndpoint extends EndpointBase {

	protected RepositoryResolver repositoryResolver;

	public ResolvableEndpoint(RepositoryInformation repoInfo, String endpoint,
			EndpointClassification endpointClassification) {
		super(repoInfo, endpoint, endpointClassification);
	}

	@Override
	public Repository getRepository() {
		if (repositoryResolver == null) {
			throw new IllegalStateException("Repository resolver not defined.");
		}
		Repository repo = repositoryResolver.getRepository(getId());
		if (repo == null) {
			throw new FedXRuntimeException("Repository with id " + getId() + " cannot be resolved.");
		}
		return repo;
	}

	public void setRepositoryResolver(RepositoryResolver repositoryResolver) {
		this.repositoryResolver = repositoryResolver;
	}
}
