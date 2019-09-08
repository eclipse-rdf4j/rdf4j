/*
 * Copyright (C) 2018 Veritas Technologies LLC.
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

import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.exception.FedXException;


/**
 * Generic interface to create {@link Endpoint}s from a repository information.
 * 
 * @author Andreas Schwarte
 *
 */
public interface EndpointProvider<T extends RepositoryInformation> {
	
	public Endpoint loadEndpoint(T repoInfo) throws FedXException;
	
}
