/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.federation;

import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

/**
 * Federated Service wrapping the {@link SPARQLRepository} to communicate with a SPARQL endpoint.
 *
 * @author Andreas Schwarte
 */
public class SPARQLFederatedService extends RepositoryFederatedService {

	private static SPARQLRepository createSPARQLRepository(String serviceUrl, HttpClientSessionManager client) {
		SPARQLRepository rep = new SPARQLRepository(serviceUrl);
		rep.setHttpClientSessionManager(client);
		return rep;
	}

	/**
	 * @param serviceUrl the serviceUrl use to initialize the inner {@link SPARQLRepository}
	 */
	public SPARQLFederatedService(String serviceUrl, HttpClientSessionManager client) {
		super(createSPARQLRepository(serviceUrl, client));
	}
}
