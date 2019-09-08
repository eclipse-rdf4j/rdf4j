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
package com.fluidops.fedx.repository;

import org.eclipse.rdf4j.repository.RepositoryResolver;

/**
 * A resolver bean that provides static access to a {@link RepositoryResolver}.
 * Is used in the initialization process via {@link FedXRepositoryFactory}.
 * 
 * <p>
 * For use in the RDF4J workbench the following Spring bean can be registered:
 * </p>
 * 
 * <pre>
 * &lt;!-- Inject the local repository manager as RepositoryResolver into FedX --&gt;
 * &lt;bean id="fedxRepositoryResolver"
 *  	class="com.fluidops.fedx.repository.FedXRepositoryResolverBean"
 *  	scope="singleton" init-method="init" destroy-method="destroy"&gt;
 *  	&lt;property name="repositoryResolver" ref="rdf4jRepositoryManager" /&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * @author Andreas Schwarte
 *
 */
public class FedXRepositoryResolverBean {

	private static RepositoryResolver repositoryResolver;

	public void init() {
	}

	public void destroy() {
	}

	public static RepositoryResolver getRepositoryResolver() {
		return repositoryResolver;
	}

	public void setRepositoryResolver(RepositoryResolver _repositoryResolver) {
		repositoryResolver = _repositoryResolver;
	}

}
