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
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.endpoint.provider.RepositoryInformation;

/**
 * A specialized {@link RepositoryEndpoint} where the lifecycle of the
 * {@link Repository} is managed by this endpoint, i.e. this instance takes care
 * for initialize and shutdown of the repository.
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
	public void initialize() throws RepositoryException {
		if (isInitialized()) {
			return;
		}
		repository.initialize();
		super.initialize();
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
