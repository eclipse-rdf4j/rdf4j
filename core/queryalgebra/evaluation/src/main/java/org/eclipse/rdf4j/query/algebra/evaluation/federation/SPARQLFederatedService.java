/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import org.eclipse.rdf4j.http.client.SesameClient;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

/**
 * Federated Service wrapping the {@link SPARQLRepository} to communicate with a
 * SPARQL endpoint.
 * 
 * @author Andreas Schwarte
 */
public class SPARQLFederatedService extends RepositoryFederatedService {

	private static SPARQLRepository createSPARQLRepository(String serviceUrl, SesameClient client)
	{
		SPARQLRepository rep = new SPARQLRepository(serviceUrl);
		rep.setSesameClient(client);
		return rep;
	}

	/**
	 * @param serviceUrl
	 *        the serviceUrl use to initialize the inner {@link SPARQLRepository}
	 */
	public SPARQLFederatedService(String serviceUrl, SesameClient client) {
		super(createSPARQLRepository(serviceUrl, client));
	}
}
