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
package org.eclipse.rdf4j.federated.endpoint.provider;

import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointClassification;
import org.eclipse.rdf4j.federated.endpoint.ResolvableEndpoint;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.repository.RepositoryResolver;

/**
 * An {@link EndpointProvider} for a {@link ResolvableEndpoint}.
 *
 * <p>
 * The federation must be initialized with a {@link RepositoryResolver} ( see
 * {@link FedXFactory#withRepositoryResolver(RepositoryResolver)}) and this resolver must offer a Repository with the id
 * provided by {@link Endpoint#getId()}
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
