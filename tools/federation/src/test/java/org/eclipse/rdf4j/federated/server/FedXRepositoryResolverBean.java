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
package org.eclipse.rdf4j.federated.server;

import org.eclipse.rdf4j.repository.RepositoryResolver;

/**
 * A resolver bean that provides static access to a {@link RepositoryResolver}.
 *
 * <p>
 * For use in the RDF4J workbench the following Spring bean can be registered:
 * </p>
 *
 * <pre>
 * &lt;!-- Inject the local repository manager as RepositoryResolver into FedX --&gt;
 * &lt;bean id="fedxRepositoryResolver"
 *  	class="org.eclipse.rdf4j.federated.server.FedXRepositoryResolverBean"
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
