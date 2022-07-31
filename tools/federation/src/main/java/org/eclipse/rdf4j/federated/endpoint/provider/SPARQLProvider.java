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
import org.eclipse.rdf4j.federated.endpoint.EndpointConfiguration;
import org.eclipse.rdf4j.federated.endpoint.ManagedRepositoryEndpoint;
import org.eclipse.rdf4j.federated.endpoint.SparqlEndpointConfiguration;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

/**
 * Provider for an Endpoint that uses a RDF4j {@link SPARQLRepository} as underlying repository. All SPARQL endpoints
 * are considered Remote.
 * <p>
 *
 * This {@link SPARQLProvider} implements special hard-coded endpoint configuration for the DBpedia endpoint: the
 * support for ASK queries is always set to false.
 *
 * @author Andreas Schwarte
 */
public class SPARQLProvider implements EndpointProvider<SPARQLRepositoryInformation> {

	@Override
	public Endpoint loadEndpoint(SPARQLRepositoryInformation repoInfo)
			throws FedXException {

		try {
			SPARQLRepository repo = new SPARQLRepository(repoInfo.getLocation());
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

			String location = repoInfo.getLocation();
			EndpointClassification epc = EndpointClassification.Remote;

			ManagedRepositoryEndpoint res = new ManagedRepositoryEndpoint(repoInfo, location, epc, repo);
			EndpointConfiguration ep = manipulateEndpointConfiguration(location, repoInfo.getEndpointConfiguration());
			res.setEndpointConfiguration(ep);

			return res;
		} catch (RepositoryException e) {
			throw new FedXException("Repository " + repoInfo.getId() + " could not be initialized: " + e.getMessage(),
					e);
		}
	}

	/**
	 * Manipulate the endpoint configuration for certain common endpoints, e.g. DBpedia => does not support ASK queries
	 *
	 * @param location
	 * @param ep
	 * @return
	 */
	private EndpointConfiguration manipulateEndpointConfiguration(String location, EndpointConfiguration ep) {

		// special hard-coded handling for DBpedia: does not support ASK
		if (location.equals("http://dbpedia.org/sparql")) {
			if (ep == null) {
				ep = new SparqlEndpointConfiguration();
			}
			if (ep instanceof SparqlEndpointConfiguration) {
				((SparqlEndpointConfiguration) ep).setSupportsASKQueries(false);
			}
		}

		return ep;
	}
}
