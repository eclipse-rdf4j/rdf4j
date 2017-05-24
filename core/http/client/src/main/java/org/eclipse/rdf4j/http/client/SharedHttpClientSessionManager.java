/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.rdf4j.http.client.util.HttpClientBuilders;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * A Manager for HTTP sessions that uses a shared {@link HttpClient} to manage HTTP connections.
 * 
 * @author James Leigh
 */
public class SharedHttpClientSessionManager implements HttpClientSessionManager, HttpClientDependent {

	/** independent life cycle */
	private volatile HttpClient httpClient;

	/** dependent life cycle */
	private volatile CloseableHttpClient dependentClient;

	private final ExecutorService executor;

	/**
	 * Optional {@link HttpClientBuilder} to create the inner {@link #httpClient} (if not provided externally)
	 */
	private volatile HttpClientBuilder httpClientBuilder;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SharedHttpClientSessionManager() {
		this.executor = Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat("rdf4j-sesameclientimpl-%d").build());
	}

	public SharedHttpClientSessionManager(CloseableHttpClient dependentClient,
			ExecutorService dependentExecutorService)
	{
		this.httpClient = this.dependentClient = Objects.requireNonNull(dependentClient,
				"HTTP client was null");
		this.executor = Objects.requireNonNull(dependentExecutorService, "Executor service was null");
	}

	@Override
	public HttpClient getHttpClient() {
		HttpClient result = httpClient;
		if (result == null) {
			synchronized (this) {
				result = httpClient;
				if (result == null) {
					result = httpClient = dependentClient = createHttpClient();
				}
			}
		}
		return result;
	}

	/**
	 * @param httpClient
	 *        The httpClient to use for remote/service calls.
	 */
	public void setHttpClient(HttpClient httpClient) {
		synchronized (this) {
			this.httpClient = Objects.requireNonNull(httpClient, "HTTP Client cannot be null");
			// If they set a client, we need to check whether we need to
			// close any existing dependentClient
			CloseableHttpClient toCloseDependentClient = dependentClient;
			dependentClient = null;
			if (toCloseDependentClient != null) {
				HttpClientUtils.closeQuietly(toCloseDependentClient);
			}
		}
	}

	/**
	 * Set an optional {@link HttpClientBuilder} to create the inner {@link #httpClient} (if the latter is not
	 * provided externally as dependent client).
	 *
	 * @param httpClientBuilder
	 *        the builder for the managed HttpClient
	 * @see HttpClientBuilders
	 */
	public void setHttpClientBuilder(HttpClientBuilder httpClientBuilder) {
		this.httpClientBuilder = httpClientBuilder;
	}

	private CloseableHttpClient createHttpClient() {
		HttpClientBuilder nextHttpClientBuilder = httpClientBuilder;
		if (nextHttpClientBuilder != null) {
			return nextHttpClientBuilder.build();
		}
		return HttpClientBuilder.create().useSystemProperties().disableAutomaticRetries().build();
	}

	@Override
	public SPARQLProtocolSession createSPARQLProtocolSession(String queryEndpointUrl, String updateEndpointUrl) {
		SPARQLProtocolSession session = new SPARQLProtocolSession(getHttpClient(), executor);
		session.setQueryURL(queryEndpointUrl);
		session.setUpdateURL(updateEndpointUrl);
		return session;
	}

	@Override
	public RDF4JProtocolSession createRDF4JProtocolSession(String serverURL) {
		RDF4JProtocolSession session = new RDF4JProtocolSession(getHttpClient(), executor);
		session.setServerURL(serverURL);
		return session;
	}

	/*-----------------*
	 * Get/set methods *
	 *-----------------*/

	@Override
	public void shutDown() {
		try {
			CloseableHttpClient toCloseDependentClient = dependentClient;
			dependentClient = null;
			if (toCloseDependentClient != null) {
				HttpClientUtils.closeQuietly(toCloseDependentClient);
			}
		}
		finally {
			try {
				executor.shutdown();
				executor.awaitTermination(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				// Preserve the interrupt status so others can check it as necessary
				Thread.currentThread().interrupt();
			}
			finally {
				if (!executor.isTerminated()) {
					executor.shutdownNow();
				}
			}
		}
	}

	/**
	 * No-op
	 * 
	 * @deprecated Create a new instance instead of trying to reactivate an old instance.
	 */
	@Deprecated
	public void initialize() {
	}

}
