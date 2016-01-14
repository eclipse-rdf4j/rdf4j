/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.rdf4j.http.client.util.HttpClientBuilders;

/**
 * Uses {@link HttpClient} to manage HTTP connections.
 * 
 * @author James Leigh
 */
public class SesameClientImpl implements SesameClient, HttpClientDependent {

	/** independent life cycle */
	private HttpClient httpClient;

	/** dependent life cycle */
	private CloseableHttpClient dependentClient;

	private ExecutorService executor = null;
	
	/**
	 * Optional {@link HttpClientBuilder} to create the inner
	 * {@link #httpClient} (if not provided externally)
	 */
	private HttpClientBuilder httpClientBuilder;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SesameClientImpl() {
		initialize();
	}

	public SesameClientImpl(CloseableHttpClient dependentClient, ExecutorService dependentExecutorService) {
		this.httpClient = this.dependentClient = dependentClient;
		this.executor = dependentExecutorService;
	}

	/**
	 * @return Returns the httpClient.
	 */
	public synchronized HttpClient getHttpClient() {
		if (httpClient == null) {
			httpClient = dependentClient = createHttpClient();
		}
		return httpClient;
	}

	/**
	 * @param httpClient The httpClient to use for remote/service calls.
	 */
	public synchronized void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Set an optional {@link HttpClientBuilder} to create the inner
	 * {@link #httpClient} (if the latter is not provided externally
	 * as dependent client).
	 *
	 * @param httpClientBuilder the builder for the managed HttpClient
	 * @see HttpClientBuilders
	 */
	public synchronized void setHttpClientBuilder(HttpClientBuilder httpClientBuilder) {
		this.httpClientBuilder = httpClientBuilder;
	}
	
	private CloseableHttpClient createHttpClient() {
		if (this.httpClientBuilder!=null) {
			return httpClientBuilder.build();
		}
		return HttpClients.createSystem();
	}

	@Override
	public synchronized SparqlSession createSparqlSession(String queryEndpointUrl, String updateEndpointUrl) {
		SparqlSession session = new SparqlSession(getHttpClient(), executor);
		session.setQueryURL(queryEndpointUrl);
		session.setUpdateURL(updateEndpointUrl);
		return session;
	}

	@Override
	public synchronized SesameSession createSesameSession(String serverURL) {
		SesameSession session = new SesameSession(getHttpClient(), executor);
		session.setServerURL(serverURL);
		return session;
	}

	/*-----------------*
	 * Get/set methods *
	 *-----------------*/

	@Override
	public synchronized void shutDown() {
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}
		if (dependentClient != null) {
			HttpClientUtils.closeQuietly(dependentClient);
			dependentClient = null;
		}
	}

	/**
	 * (re)initializes the connection manager and HttpClient (if not already
	 * done), for example after a shutdown has been invoked earlier. Invoking
	 * this method multiple times will have no effect.
	 */
	public synchronized void initialize() {
		if (executor == null) {
			executor = Executors.newCachedThreadPool();
		}
	}

}
