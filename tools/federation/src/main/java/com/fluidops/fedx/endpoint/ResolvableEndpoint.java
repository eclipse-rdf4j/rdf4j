/*
 * Copyright (C) 2019 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.endpoint;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryResolver;

import com.fluidops.fedx.endpoint.provider.RepositoryInformation;
import com.fluidops.fedx.exception.FedXRuntimeException;

/**
 * A specialized {@link Endpoint} that allows to resolve the repository using a
 * {@link RepositoryResolver}.
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
