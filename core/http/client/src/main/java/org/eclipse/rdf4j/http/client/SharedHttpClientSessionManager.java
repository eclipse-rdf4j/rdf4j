/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpConnection;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.eclipse.rdf4j.http.client.util.HttpClientBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Manager for HTTP sessions that uses a shared {@link HttpClient} to manage HTTP connections.
 *
 * @author James Leigh
 */
public class SharedHttpClientSessionManager implements HttpClientSessionManager, HttpClientDependent {
	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.executors.corePoolSize} for specifying the
	 * background executor core thread pool size.
	 */
	public static final String CORE_POOL_SIZE_PROPERTY = "org.eclipse.rdf4j.client.executors.corePoolSize";

	private static final AtomicLong threadCount = new AtomicLong();

	private final Logger logger = LoggerFactory.getLogger(SharedHttpClientSessionManager.class);

	/** independent life cycle */
	private volatile HttpClient httpClient;

	/** dependent life cycle */
	private volatile CloseableHttpClient dependentClient;

	private final ExecutorService executor;

	/**
	 * Optional {@link HttpClientBuilder} to create the inner {@link #httpClient} (if not provided externally)
	 */
	private volatile HttpClientBuilder httpClientBuilder;

	private final Map<SPARQLProtocolSession, Boolean> openSessions = new ConcurrentHashMap<>();

	private static final HttpRequestRetryHandler retryHandlerStale = new RetryHandlerStale();

	/**
	 * Retry handler: closes stale connections and suggests to simply retry the HTTP request once. Just closing the
	 * stale connection is enough: the connection will be reopened elsewhere. This seems to be necessary for Jetty
	 * 9.4.24+.
	 * 
	 * Other HTTP issues are considered to be more severe, so these requests are not retried.
	 */
	private static class RetryHandlerStale implements HttpRequestRetryHandler {
		private final Logger logger = LoggerFactory.getLogger(RetryHandlerStale.class);

		@Override
		public boolean retryRequest(IOException ioe, int count, HttpContext context) {
			// only try this once
			if (count > 1) {
				return false;
			}
			HttpClientContext clientContext = HttpClientContext.adapt(context);
			HttpConnection conn = clientContext.getConnection();

			synchronized (this) {
				if (conn.isStale()) {
					try {
						logger.warn("Closing stale connection");
						conn.close();
						return true;
					} catch (IOException e) {
						logger.error("Error closing stale connection", e);
					}
				}
			}
			return false;
		}
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SharedHttpClientSessionManager() {
		final ThreadFactory backingThreadFactory = Executors.defaultThreadFactory();

		ExecutorService threadPoolExecutor = Executors.newCachedThreadPool((Runnable runnable) -> {
			Thread thread = backingThreadFactory.newThread(runnable);
			thread.setName(
					String.format("rdf4j-SharedHttpClientSessionManager-%d", threadCount.getAndIncrement()));
			thread.setDaemon(true);
			return thread;
		});

		Integer corePoolSize = Integer.getInteger(CORE_POOL_SIZE_PROPERTY, 1);
		((ThreadPoolExecutor) threadPoolExecutor).setCorePoolSize(corePoolSize);
		this.executor = threadPoolExecutor;
	}

	public SharedHttpClientSessionManager(CloseableHttpClient dependentClient,
			ScheduledExecutorService dependentExecutorService) {
		this.httpClient = this.dependentClient = Objects.requireNonNull(dependentClient, "HTTP client was null");
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
	 * @param httpClient The httpClient to use for remote/service calls.
	 */
	@Override
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
	 * Set an optional {@link HttpClientBuilder} to create the inner {@link #httpClient} (if the latter is not provided
	 * externally as dependent client).
	 *
	 * @param httpClientBuilder the builder for the managed HttpClient
	 * @see HttpClientBuilders
	 */
	public void setHttpClientBuilder(HttpClientBuilder httpClientBuilder) {
		this.httpClientBuilder = httpClientBuilder;
	}

	@Override
	public SPARQLProtocolSession createSPARQLProtocolSession(String queryEndpointUrl, String updateEndpointUrl) {
		SPARQLProtocolSession session = new SPARQLProtocolSession(getHttpClient(), executor) {

			@Override
			public void close() {
				try {
					super.close();
				} finally {
					openSessions.remove(this);
				}
			}
		};
		session.setQueryURL(queryEndpointUrl);
		session.setUpdateURL(updateEndpointUrl);
		openSessions.put(session, true);
		return session;
	}

	@Override
	public RDF4JProtocolSession createRDF4JProtocolSession(String serverURL) {
		RDF4JProtocolSession session = new RDF4JProtocolSession(getHttpClient(), executor) {

			@Override
			public void close() {
				try {
					super.close();
				} finally {
					openSessions.remove(this);
				}
			}
		};
		session.setServerURL(serverURL);
		openSessions.put(session, true);
		return session;
	}

	@Override
	public void shutDown() {
		try {
			openSessions.keySet().forEach(session -> {
				try {
					session.close();
				} catch (Exception e) {
					logger.error(e.toString(), e);
				}
			});
			CloseableHttpClient toCloseDependentClient = dependentClient;
			dependentClient = null;
			if (toCloseDependentClient != null) {
				HttpClientUtils.closeQuietly(toCloseDependentClient);
			}
		} finally {
			try {
				executor.shutdown();
				executor.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				// Preserve the interrupt status so others can check it as necessary
				Thread.currentThread().interrupt();
			} finally {
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

	/**
	 * Get the {@link ExecutorService} used by this session manager.
	 *
	 * @return a {@link ExecutorService} used by all {@link SPARQLProtocolSession} and {@link RDF4JProtocolSession}
	 *         instances created by this session manager.
	 */
	protected final ExecutorService getExecutorService() {
		return this.executor;
	}

	private CloseableHttpClient createHttpClient() {
		HttpClientBuilder nextHttpClientBuilder = httpClientBuilder;
		if (nextHttpClientBuilder != null) {
			return nextHttpClientBuilder.build();
		}

		return HttpClientBuilder.create()
				.evictExpiredConnections()
				.setRetryHandler(retryHandlerStale)
				.useSystemProperties()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
				.build();
	}
}
