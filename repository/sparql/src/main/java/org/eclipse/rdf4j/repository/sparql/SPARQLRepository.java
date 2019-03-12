/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.SessionManagerDependent;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.AbstractRepository;

/**
 * A proxy class to access any SPARQL endpoint. The instance must be initialized prior to using it.
 * 
 * @author James Leigh
 */
public class SPARQLRepository extends AbstractRepository implements HttpClientDependent, SessionManagerDependent {

	/**
	 * Flag indicating if quad mode is enabled in newly created {@link SPARQLConnection}s.
	 * 
	 * @see #enableQuadMode(boolean)
	 */
	private boolean quadMode = false;

	/**
	 * The HTTP client that takes care of the client-server communication.
	 */
	private volatile HttpClientSessionManager client;

	/** dependent life cycle */
	private volatile SharedHttpClientSessionManager dependentClient;

	private String username;

	private String password;

	private final String queryEndpointUrl;

	private final String updateEndpointUrl;

	private volatile Map<String, String> additionalHttpHeaders = Collections.emptyMap();

	/**
	 * Create a new SPARQLRepository using the supplied endpoint URL for queries and updates.
	 * 
	 * @param endpointUrl a SPARQL endpoint URL. May not be null.
	 */
	public SPARQLRepository(String endpointUrl) {
		this(endpointUrl, endpointUrl);
	}

	/**
	 * Create a new SPARQLREpository using the supplied query endpoint URL for queries, and the supplied update endpoint
	 * URL for updates.
	 * 
	 * @param queryEndpointUrl  a SPARQL endpoint URL for queries. May not be null.
	 * @param updateEndpointUrl a SPARQL endpoint URL for updates. May not be null.
	 * @throws IllegalArgumentException if one of the supplied endpoint URLs is null.
	 */
	public SPARQLRepository(String queryEndpointUrl, String updateEndpointUrl) {
		if (queryEndpointUrl == null || updateEndpointUrl == null) {
			throw new IllegalArgumentException("endpoint URL may not be null.");
		}
		this.queryEndpointUrl = queryEndpointUrl;
		this.updateEndpointUrl = updateEndpointUrl;
	}

	@Override
	public HttpClientSessionManager getHttpClientSessionManager() {
		HttpClientSessionManager result = client;
		if (result == null) {
			synchronized (this) {
				result = client;
				if (result == null) {
					result = client = dependentClient = new SharedHttpClientSessionManager();
				}
			}
		}
		return result;
	}

	@Override
	public void setHttpClientSessionManager(HttpClientSessionManager client) {
		synchronized (this) {
			this.client = client;
			// If they set a client, we need to check whether we need to shutdown any existing dependentClient
			SharedHttpClientSessionManager toCloseDependentClient = dependentClient;
			dependentClient = null;
			if (toCloseDependentClient != null) {
				toCloseDependentClient.shutDown();
			}
		}
	}

	@Override
	public final HttpClient getHttpClient() {
		return getHttpClientSessionManager().getHttpClient();
	}

	@Override
	public void setHttpClient(HttpClient httpClient) {
		SharedHttpClientSessionManager toSetDependentClient = dependentClient;
		if (toSetDependentClient == null) {
			getHttpClientSessionManager();
			toSetDependentClient = dependentClient;
		}
		// The strange lifecycle results in the possibility that the
		// dependentClient will be null due to a call to setSesameClient, so add
		// a null guard here for that possibility
		if (toSetDependentClient != null) {
			toSetDependentClient.setHttpClient(httpClient);
		}
	}

	/**
	 * Creates a new HTTPClient object. Subclasses may override to return a more specific HTTPClient subtype.
	 * 
	 * @return a HTTPClient object.
	 */
	protected SPARQLProtocolSession createHTTPClient() {
		// initialize HTTP client
		SPARQLProtocolSession httpClient = getHttpClientSessionManager().createSPARQLProtocolSession(queryEndpointUrl,
				updateEndpointUrl);
		httpClient.setValueFactory(SimpleValueFactory.getInstance());
		httpClient.setPreferredTupleQueryResultFormat(TupleQueryResultFormat.SPARQL);
		httpClient.setAdditionalHttpHeaders(additionalHttpHeaders);
		if (username != null) {
			httpClient.setUsernameAndPassword(username, password);
		}
		return httpClient;
	}

	@Override
	public RepositoryConnection getConnection() throws RepositoryException {
		if (!isInitialized()) {
			throw new RepositoryException("SPARQLRepository not initialized.");
		}
		return new SPARQLConnection(this, createHTTPClient(), quadMode);
	}

	@Override
	public File getDataDir() {
		return null;
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	protected void initializeInternal() throws RepositoryException {
		// no-op
	}

	@Override
	public boolean isWritable() throws RepositoryException {
		return false;
	}

	@Override
	public void setDataDir(File dataDir) {
		// no-op
	}

	/**
	 * Set the username and password to use for authenticating with the remote repository.
	 * 
	 * @param username the username. Setting this to null will disable authentication.
	 * @param password the password. Setting this to null will disable authentication.
	 */
	public void setUsernameAndPassword(final String username, final String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	protected void shutDownInternal() throws RepositoryException {
		try {
			SharedHttpClientSessionManager toCloseDependentClient = dependentClient;
			dependentClient = null;
			if (toCloseDependentClient != null) {
				toCloseDependentClient.shutDown();
			}
		} finally {
			// remove reference but do not shut down, client may be shared by
			// other repos.
			client = null;
		}
	}

	@Override
	public String toString() {
		return queryEndpointUrl;
	}

	/**
	 * Get the additional HTTP headers which will be used
	 * 
	 * @return a read-only view of the additional HTTP headers which will be included in every request to the server.
	 */
	public Map<String, String> getAdditionalHttpHeaders() {
		return Collections.unmodifiableMap(additionalHttpHeaders);
	}

	/**
	 * Set additional HTTP headers to be included in every request to the server, which may be required for certain
	 * unusual server configurations. This will only take effect on connections subsequently returned by
	 * {@link #getConnection()}.
	 * 
	 * @param additionalHttpHeaders a map containing pairs of header names and values. May be null
	 */
	public void setAdditionalHttpHeaders(Map<String, String> additionalHttpHeaders) {
		if (additionalHttpHeaders == null) {
			this.additionalHttpHeaders = Collections.emptyMap();
		} else {
			this.additionalHttpHeaders = additionalHttpHeaders;
		}
	}

	/**
	 * Activate quad mode for this {@link SPARQLRepository}, i.e. for retrieval of statements also retrieve the graph.
	 * <p>
	 * Note: the setting is only applied in newly created {@link SPARQLConnection}s as the setting is an immutable
	 * configuration of a connection instance.
	 * 
	 * @param flag flag to enable or disable the quad mode
	 * @see SPARQLConnection#getStatements(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.URI,
	 *      org.eclipse.rdf4j.model.Value, boolean, org.eclipse.rdf4j.model.Resource...)
	 */
	public void enableQuadMode(boolean flag) {
		this.quadMode = flag;
	}
}
