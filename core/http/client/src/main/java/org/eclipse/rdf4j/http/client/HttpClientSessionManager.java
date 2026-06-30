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
package org.eclipse.rdf4j.http.client;

import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;

/**
 * Manager for remote HTTP sessions using a {@link RDF4JHttpClient}.
 *
 * @author James Leigh
 */
public interface HttpClientSessionManager {

	/**
	 * @return Returns the httpClient.
	 */
	RDF4JHttpClient getHttpClient();

	/**
	 * Assign an {@link RDF4JHttpClient} that this session manager should use.
	 *
	 * @param client the client to use.
	 */
	void setHttpClient(RDF4JHttpClient client);

	/**
	 * Creates a new SPARQL Protocol session to the remote SPARQL endpoint.
	 */
	SPARQLProtocolSession createSPARQLProtocolSession(String queryEndpointUrl, String updateEndpointUrl);

	/**
	 * Creates a new session to the remote RDF4J REST API.
	 */
	RDF4JProtocolSession createRDF4JProtocolSession(String serverURL);

	/**
	 * Closes any remaining connections and threads used by the sessions created by this object.
	 */
	void shutDown();

}
