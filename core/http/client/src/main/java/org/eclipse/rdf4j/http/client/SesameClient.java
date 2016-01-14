/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import org.apache.http.client.HttpClient;

/**
 * Manages remote HTP connections.
 * 
 * @author James Leigh
 */
public interface SesameClient {

	/**
	 * @return Returns the httpClient.
	 */
	HttpClient getHttpClient();

	/**
	 * Creates a new session to the remote SPARQL endpoint to manage the auth
	 * state.
	 */
	SparqlSession createSparqlSession(String queryEndpointUrl, String updateEndpointUrl);

	/**
	 * Creates a new session to the remote Sesame server to manage the auth
	 * state.
	 */
	SesameSession createSesameSession(String serverURL);

	/**
	 * Closes any remaining TCP connections and threads used by the sessions
	 * created by this object.
	 */
	void shutDown();

}
