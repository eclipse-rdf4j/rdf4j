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
import org.eclipse.rdf4j.repository.Repository;

/**
 * A specialized {@link Endpoint} that has a reference to a configured {@link Repository}.
 *
 * <p>
 * Note that this implementation does not take care for the lifecycle of the repository. If the lifecycle of the
 * {@link Repository} should be managed by FedX, consider using {@link ManagedRepositoryEndpoint}
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public class RepositoryEndpoint extends EndpointBase {

	protected final Repository repository;

	public RepositoryEndpoint(RepositoryInformation repoInfo, String endpoint,
			EndpointClassification endpointClassification, Repository repository) {
		super(repoInfo, endpoint, endpointClassification);
		this.repository = repository;
	}

	@Override
	public Repository getRepository() {
		return repository;
	}
}
