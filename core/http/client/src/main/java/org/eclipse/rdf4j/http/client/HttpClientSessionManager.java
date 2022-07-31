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

import org.apache.http.client.HttpClient;

/**
 * Manager for remote HTTP sessions using a {@link HttpClient}.
 *
 * @author James Leigh
 */
@SuppressWarnings("deprecation")
public interface HttpClientSessionManager {

	/**
	 * @return Returns the httpClient.
	 */
	HttpClient getHttpClient();

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
