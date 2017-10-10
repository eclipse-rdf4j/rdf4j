/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import org.apache.http.client.HttpClient;

/**
 * @author Jeen Broekstra
 * @deprecated use {@link HttpClientSessionManager} instead.
 */
@Deprecated
public interface SesameClient {

	/**
	 * @return Returns the httpClient.
	 */
	HttpClient getHttpClient();

	/**
	 * Creates a new session to the remote SPARQL endpoint to manage the auth state.
	 */
	SPARQLProtocolSession createSPARQLProtocolSession(String queryEndpointUrl, String updateEndpointUrl);

	/**
	 * @deprecated use {@link #createSPARQLProtocolSession(String, String)} instead.
	 */
	@Deprecated
	default SPARQLProtocolSession createSparqlSession(String queryEndpointUrl, String updateEndpointUrl) {
		return createSPARQLProtocolSession(queryEndpointUrl, updateEndpointUrl);
	}

	/**
	 * Creates a new session to the remote Sesame server to manage the auth state.
	 */
	RDF4JProtocolSession createRDF4JProtocolSession(String serverURL);

	/**
	 * @deprecated use {@link #createRDF4JProtocolSession(String)} instead.
	 */
	@Deprecated
	default RDF4JProtocolSession createSesameSession(String serverURL) {
		return createRDF4JProtocolSession(serverURL);
	}

	/**
	 * Closes any remaining connections and threads used by the sessions created by this object.
	 */
	void shutDown();
}
