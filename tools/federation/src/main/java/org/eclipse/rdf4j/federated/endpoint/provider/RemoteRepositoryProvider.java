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

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointClassification;
import org.eclipse.rdf4j.federated.endpoint.ManagedRepositoryEndpoint;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

/**
 * Provider for an Endpoint that uses a RDF4J {@link HTTPRepository} as underlying repository. All SPARQL endpoints are
 * considered Remote.
 *
 * @author Andreas Schwarte
 */
public class RemoteRepositoryProvider implements EndpointProvider<RemoteRepositoryRepositoryInformation> {

	@Override
	public Endpoint loadEndpoint(RemoteRepositoryRepositoryInformation repoInfo)
			throws FedXException {

		String repositoryServer = repoInfo.get("repositoryServer");
		String repositoryName = repoInfo.get("repositoryName");

		if (repositoryServer == null || repositoryName == null) {
			throw new FedXException("Invalid configuration, repositoryServer and repositoryName are required for "
					+ repoInfo.getName());
		}

		try {
			HTTPRepository repo = new HTTPRepository(repositoryServer, repositoryName);
			HttpClientBuilder httpClientBuilder = HttpClients.custom()
					.useSystemProperties()
					.setMaxConnTotal(20)
					.setMaxConnPerRoute(20);
			((SharedHttpClientSessionManager) repo.getHttpClientSessionManager())
					.setHttpClientBuilder(httpClientBuilder);
			try {
				repo.init();
			} finally {
				repo.shutDown();
			}

			String location = repositoryServer + "/" + repositoryName;
			EndpointClassification epc = EndpointClassification.Remote;

			ManagedRepositoryEndpoint res = new ManagedRepositoryEndpoint(repoInfo, location, epc, repo);
			res.setEndpointConfiguration(repoInfo.getEndpointConfiguration());

			return res;
		} catch (Exception e) {
			throw new FedXException("Repository " + repoInfo.getId() + " could not be initialized: " + e.getMessage(),
					e);
		}
	}

}
