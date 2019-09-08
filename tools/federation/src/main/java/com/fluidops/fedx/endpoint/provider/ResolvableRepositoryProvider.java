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
package com.fluidops.fedx.endpoint.provider;

import org.eclipse.rdf4j.repository.RepositoryResolver;

import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.endpoint.EndpointClassification;
import com.fluidops.fedx.endpoint.ResolvableEndpoint;
import com.fluidops.fedx.exception.FedXException;

/**
 * An {@link EndpointProvider} for a {@link ResolvableEndpoint}.
 * 
 * <p>
 * The federation must be initialized with a {@link RepositoryResolver} ( see
 * {@link FedXFactory#withRepositoryResolver(RepositoryResolver)}) and this
 * resolver must offer a Repository with the id provided by
 * {@link Endpoint#getId()}
 * </p>
 * 
 * @author Andreas Schwarte
 *
 */
public class ResolvableRepositoryProvider implements EndpointProvider<ResolvableRepositoryInformation> {

	@Override
	public Endpoint loadEndpoint(ResolvableRepositoryInformation repoInfo) throws FedXException {

		return new ResolvableEndpoint(repoInfo, repoInfo.getLocation(), EndpointClassification.Remote);
	}

}
