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

import com.fluidops.fedx.endpoint.provider.RepositoryInformation;

/**
 * A specialized {@link Endpoint} that has a reference to a configured
 * {@link Repository}.
 * 
 * <p>
 * Note that this implementation does not take care for the lifecycle of the
 * repository. If the lifecycle of the {@link Repository} should be managed by
 * FedX, consider using {@link ManagedRepositoryEndpoint}
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
