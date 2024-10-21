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

import java.io.IOException;
import java.net.HttpURLConnection;
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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
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

	private static final AtomicLong threadCount = new AtomicLong();

	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.executors.corePoolSize} for specifying the
	 * background executor core thread pool size.
	 */
	public static final String CORE_POOL_SIZE_PROPERTY = "org.eclipse.rdf4j.client.executors.corePoolSize";

	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.http.maxConnPerRoute} for specifying the maximum
	 * number of connections per route (per host). Default is 25.
	 *
	 * <p>
	 * This property determines the maximum number of concurrent connections to a single host (route). Adjusting this
	 * value can improve performance when communicating with a server that supports multiple concurrent connections.
	 * </p>
	 */
	public static final String MAX_CONN_PER_ROUTE_PROPERTY = "org.eclipse.rdf4j.client.http.maxConnPerRoute";

	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.http.maxConnTotal} for specifying the maximum total
	 * number of connections. Default is 50.
	 *
	 * <p>
	 * This property sets the maximum total number of concurrent connections that can be open at the same time.
	 * Increasing this value allows more simultaneous connections to different hosts, which can improve throughput in
	 * multi-threaded environments.
	 * </p>
	 */
	public static final String MAX_CONN_TOTAL_PROPERTY = "org.eclipse.rdf4j.client.http.maxConnTotal";

	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.http.connectionTimeout} for specifying the HTTP
	 * connection timeout in milliseconds for general use. Default is 30 seconds.
	 *
	 * <p>
	 * The connection timeout determines the maximum time the client will wait to establish a TCP connection to the
	 * server. A default of 30 seconds is set to allow for potential network delays without causing unnecessary
	 * timeouts.
	 * </p>
	 */
	public static final String CONNECTION_TIMEOUT_PROPERTY = "org.eclipse.rdf4j.client.http.connectionTimeout";

	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.http.connectionRequestTimeout} for specifying the
	 * HTTP connection request timeout in milliseconds for general use. Default is 1 hour.
	 *
	 * <p>
	 * The connection request timeout defines how long the client will wait for a connection from the connection pool.
	 * </p>
	 */
	public static final String CONNECTION_REQUEST_TIMEOUT_PROPERTY = "org.eclipse.rdf4j.client.http.connectionRequestTimeout";

	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.http.socketTimeout} for specifying the HTTP socket
	 * timeout in milliseconds for general use. Default is 10 days.
	 *
	 * <p>
	 * The socket timeout controls the maximum period of inactivity between data packets during data transfer. A longer
	 * timeout is appropriate for large data transfers, ensuring that operations are not interrupted prematurely.
	 * </p>
	 */
	public static final String SOCKET_TIMEOUT_PROPERTY = "org.eclipse.rdf4j.client.http.socketTimeout";

	// System property constants for SPARQL SERVICE timeouts

	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.sparql.http.connectionTimeout} for specifying the
	 * HTTP connection timeout in milliseconds when used in SPARQL SERVICE calls. Default is 5 seconds.
	 *
	 * <p>
	 * A shorter connection timeout is set for SPARQL SERVICE calls to quickly detect unresponsive endpoints in
	 * federated queries, improving overall query performance by avoiding long waits for unreachable servers.
	 * </p>
	 */
	public static final String SPARQL_CONNECTION_TIMEOUT_PROPERTY = "org.eclipse.rdf4j.client.sparql.http.connectionTimeout";

	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.sparql.http.connectionRequestTimeout} for specifying
	 * the HTTP connection request timeout in milliseconds when used in SPARQL SERVICE calls. Default is 10 minutes.
	 *
	 * <p>
	 * This timeout controls how long the client waits for a connection from the pool when making SPARQL SERVICE calls.
	 * A shorter timeout than general use ensures that queries fail fast if resources are constrained, maintaining
	 * responsiveness.
	 * </p>
	 */
	public static final String SPARQL_CONNECTION_REQUEST_TIMEOUT_PROPERTY = "org.eclipse.rdf4j.client.sparql.http.connectionRequestTimeout";

	/**
	 * Configurable system property {@code org.eclipse.rdf4j.client.sparql.http.socketTimeout} for specifying the HTTP
	 * socket timeout in milliseconds when used in SPARQL SERVICE calls. Default is 1 hour.
	 *
	 * <p>
	 * The socket timeout for SPARQL SERVICE calls is set to a shorter duration to detect unresponsive servers during
	 * data transfer, ensuring that the client does not wait indefinitely for data that may never arrive.
	 * </p>
	 */
	public static final String SPARQL_SOCKET_TIMEOUT_PROPERTY = "org.eclipse.rdf4j.client.sparql.http.socketTimeout";

	// Defaults

	/**
	 * Default core pool size for the executor service. Set to 5.
	 *
	 * <p>
	 * This value determines the number of threads to keep in the pool, even if they are idle. Adjusting this value can
	 * help manage resource utilization in high-load scenarios.
	 * </p>
	 */
	public static final int DEFAULT_CORE_POOL_SIZE = 5;

	/**
	 * Default maximum number of connections per route (per host). Set to 25.
	 *
	 * <p>
	 * This value limits the number of concurrent connections to a single host. Increasing it can improve performance
	 * when communicating with a server that can handle multiple connections.
	 * </p>
	 */
	public static final int DEFAULT_MAX_CONN_PER_ROUTE = 25;

	/**
	 * Default maximum total number of connections. Set to 50.
	 *
	 * <p>
	 * This value limits the total number of concurrent connections that can be open at the same time. Increasing it
	 * allows for more simultaneous connections to different hosts.
	 * </p>
	 */
	public static final int DEFAULT_MAX_CONN_TOTAL = 50;

	/**
	 * Default HTTP connection timeout in milliseconds for general use. Set to 30 seconds.
	 *
	 * <p>
	 * The connection timeout determines the maximum time the client will wait to establish a TCP connection to the
	 * server.
	 * </p>
	 */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 30 * 1000; // 30 seconds

	/**
	 * Default HTTP connection request timeout in milliseconds for general use. Set to 1 hour.
	 *
	 * <p>
	 * The connection request timeout defines how long the client will wait for a connection from the connection pool.
	 * </p>
	 */
	public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 60 * 60 * 1000; // 1 hour

	/**
	 * Default HTTP socket timeout in milliseconds for general use. Set to 10 days.
	 *
	 * <p>
	 * The socket timeout controls the maximum period of inactivity between data packets during data transfer. A longer
	 * timeout is appropriate for large data transfers.
	 * </p>
	 */
	public static final int DEFAULT_SOCKET_TIMEOUT = 10 * 24 * 60 * 60 * 1000; // 10 days

	// Default timeout values for SPARQL SERVICE calls

	/**
	 * Default HTTP connection timeout in milliseconds for SPARQL SERVICE calls. Set to 5 seconds.
	 *
	 * <p>
	 * A shorter connection timeout is set for SPARQL SERVICE calls to quickly detect unresponsive endpoints in
	 * federated queries.
	 * </p>
	 */
	public static final int DEFAULT_SPARQL_CONNECTION_TIMEOUT = 5 * 1000; // 5 seconds

	/**
	 * Default HTTP connection request timeout in milliseconds for SPARQL SERVICE calls. Set to 10 minutes.
	 *
	 * <p>
	 * This timeout controls how long the client waits for a connection from the pool when making SPARQL SERVICE calls.
	 * </p>
	 */
	public static final int DEFAULT_SPARQL_CONNECTION_REQUEST_TIMEOUT = 10 * 60 * 1000; // 10 minutes

	/**
	 * Default HTTP socket timeout in milliseconds for SPARQL SERVICE calls. Set to 1 hour.
	 *
	 * <p>
	 * The socket timeout for SPARQL SERVICE calls is set to a shorter duration to detect unresponsive servers during
	 * data transfer.
	 * </p>
	 */
	public static final int DEFAULT_SPARQL_SOCKET_TIMEOUT = 60 * 60 * 1000; // 1 hour

	// Values as read from system properties or defaults

	/**
	 * Core pool size for the executor service, as read from system properties or defaults.
	 */
	public static final int CORE_POOL_SIZE = Integer
			.parseInt(System.getProperty(CORE_POOL_SIZE_PROPERTY, String.valueOf(DEFAULT_CORE_POOL_SIZE)));

	/**
	 * Maximum number of connections per route (per host), as read from system properties or defaults.
	 */
	public static final int MAX_CONN_PER_ROUTE = Integer
			.parseInt(System.getProperty(MAX_CONN_PER_ROUTE_PROPERTY, String.valueOf(DEFAULT_MAX_CONN_PER_ROUTE)));

	/**
	 * Maximum total number of connections, as read from system properties or defaults.
	 */
	public static final int MAX_CONN_TOTAL = Integer
			.parseInt(System.getProperty(MAX_CONN_TOTAL_PROPERTY, String.valueOf(DEFAULT_MAX_CONN_TOTAL)));

	/**
	 * HTTP connection timeout in milliseconds for general use.
	 */
	public static final int CONNECTION_TIMEOUT = Integer.parseInt(
			System.getProperty(CONNECTION_TIMEOUT_PROPERTY, String.valueOf(DEFAULT_CONNECTION_TIMEOUT)));

	/**
	 * HTTP connection request timeout in milliseconds for general use.
	 */
	public static final int CONNECTION_REQUEST_TIMEOUT = Integer.parseInt(
			System.getProperty(CONNECTION_REQUEST_TIMEOUT_PROPERTY,
					String.valueOf(DEFAULT_CONNECTION_REQUEST_TIMEOUT)));

	/**
	 * HTTP socket timeout in milliseconds for general use.
	 */
	public static final int SOCKET_TIMEOUT = Integer
			.parseInt(System.getProperty(SOCKET_TIMEOUT_PROPERTY, String.valueOf(DEFAULT_SOCKET_TIMEOUT)));

	/**
	 * HTTP connection timeout in milliseconds for SPARQL SERVICE calls.
	 */
	public static final int SPARQL_CONNECTION_TIMEOUT = Integer.parseInt(
			System.getProperty(SPARQL_CONNECTION_TIMEOUT_PROPERTY, String.valueOf(DEFAULT_SPARQL_CONNECTION_TIMEOUT)));

	/**
	 * HTTP connection request timeout in milliseconds for SPARQL SERVICE calls.
	 */
	public static final int SPARQL_CONNECTION_REQUEST_TIMEOUT = Integer.parseInt(
			System.getProperty(SPARQL_CONNECTION_REQUEST_TIMEOUT_PROPERTY,
					String.valueOf(DEFAULT_SPARQL_CONNECTION_REQUEST_TIMEOUT)));

	/**
	 * HTTP socket timeout in milliseconds for SPARQL SERVICE calls.
	 */
	public static final int SPARQL_SOCKET_TIMEOUT = Integer.parseInt(
			System.getProperty(SPARQL_SOCKET_TIMEOUT_PROPERTY, String.valueOf(DEFAULT_SPARQL_SOCKET_TIMEOUT)));

	// Variables for the currently used timeouts

	private int currentConnectionTimeout = CONNECTION_TIMEOUT;
	private int currentConnectionRequestTimeout = CONNECTION_REQUEST_TIMEOUT;
	private int currentSocketTimeout = SOCKET_TIMEOUT;

	private final Logger logger = LoggerFactory.getLogger(SharedHttpClientSessionManager.class);

	/**
	 * Independent life cycle
	 */
	private volatile HttpClient httpClient;

	/**
	 * Dependent life cycle
	 */
	private volatile CloseableHttpClient dependentClient;

	private final ExecutorService executor;

	/**
	 * Optional {@link HttpClientBuilder} to create the inner {@link #httpClient} (if not provided externally)
	 */
	private volatile HttpClientBuilder httpClientBuilder;

	private final Map<SPARQLProtocolSession, Boolean> openSessions = new ConcurrentHashMap<>();

	private static final HttpRequestRetryHandler retryHandlerStale = new RetryHandlerStale();
	private static final ServiceUnavailableRetryStrategy serviceUnavailableRetryHandler = new ServiceUnavailableRetryHandler();

	/**
	 * Retry handler: closes stale connections and suggests to simply retry the HTTP request once. Just closing the
	 * stale connection is enough: the connection will be reopened elsewhere. This seems to be necessary for Jetty
	 * 9.4.24+.
	 * <p>
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
			if (conn != null) {
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
			}
			return false;
		}
	}

	private static class ServiceUnavailableRetryHandler implements ServiceUnavailableRetryStrategy {
		private final Logger logger = LoggerFactory.getLogger(ServiceUnavailableRetryHandler.class);

		@Override
		public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
			// only retry on HTTP 408 (Request Timeout)
			if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_CLIENT_TIMEOUT) {
				return false;
			}

			// when `keepAlive` is disabled every connection is fresh (with the default `useSystemProperties` http
			// client configuration we use), a 408 in that case is an unexpected issue we don't handle here
			String keepAlive = System.getProperty("http.keepAlive", "true");
			if (!"true".equalsIgnoreCase(keepAlive)) {
				return false;
			}

			// Worst case, the connection pool is filled to the max and all of them idled out on the server already
			// We then need to clean up the pool and retry with a fresh connection. Hence, we need at most
			// pooledConnections + 1 retries.
			int pooledConnections = MAX_CONN_PER_ROUTE;
			if (executionCount > (pooledConnections + 1)) {
				return false;
			}

			HttpClientContext clientContext = HttpClientContext.adapt(context);
			HttpConnection conn = clientContext.getConnection();

			synchronized (this) {
				try {
					logger.info("Cleaning up closed connection");
					conn.close();
					return true;
				} catch (IOException e) {
					logger.error("Error cleaning up closed connection", e);
				}
			}
			return false;
		}

		@Override
		public long getRetryInterval() {
			return 1000;
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

		Integer corePoolSize = CORE_POOL_SIZE;
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
			// Close open sessions
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
			// Shutdown the executor
			try {
				executor.shutdown();
				executor.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
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

		RequestConfig requestConfig = getDefaultRequestConfig();

		return HttpClientBuilder.create()
				.evictExpiredConnections()
				.evictIdleConnections(30, TimeUnit.MINUTES)
				.setRetryHandler(retryHandlerStale)
				.setServiceUnavailableRetryStrategy(serviceUnavailableRetryHandler)
				.setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
				.setMaxConnTotal(MAX_CONN_TOTAL)
				.useSystemProperties()
				.setDefaultRequestConfig(requestConfig)
				.build();
	}

	/**
	 * Returns the default {@link RequestConfig} using the currently set timeout values.
	 *
	 * @return a configured {@link RequestConfig} with the current timeouts.
	 */
	public RequestConfig getDefaultRequestConfig() {
		return RequestConfig.custom()
				.setConnectTimeout(currentConnectionTimeout)
				.setConnectionRequestTimeout(currentConnectionRequestTimeout)
				.setSocketTimeout(currentSocketTimeout)
				.setCookieSpec(CookieSpecs.STANDARD)
				.build();
	}

	/**
	 * Switches the current timeout settings to use the SPARQL-specific timeouts. This method should be called when
	 * making SPARQL SERVICE calls to apply shorter timeout values.
	 *
	 * <p>
	 * The SPARQL-specific timeouts are shorter to ensure that unresponsive or slow SPARQL endpoints do not cause long
	 * delays in federated query processing. Quick detection of such issues improves the responsiveness and reliability
	 * of SPARQL queries.
	 * </p>
	 */
	public void setDefaultSparqlServiceTimeouts() {
		this.currentConnectionTimeout = SPARQL_CONNECTION_TIMEOUT;
		this.currentConnectionRequestTimeout = SPARQL_CONNECTION_REQUEST_TIMEOUT;
		this.currentSocketTimeout = SPARQL_SOCKET_TIMEOUT;
	}

	/**
	 * Resets the current timeout settings to the general timeouts. This method should be called to revert any changes
	 * made by {@link #setDefaultSparqlServiceTimeouts()} and apply the general timeout values.
	 *
	 * <p>
	 * The general timeouts are longer to accommodate operations that may take more time, such as large data transfers
	 * or extensive processing, without causing premature timeouts.
	 * </p>
	 */
	public void setDefaultTimeouts() {
		this.currentConnectionTimeout = CONNECTION_TIMEOUT;
		this.currentConnectionRequestTimeout = CONNECTION_REQUEST_TIMEOUT;
		this.currentSocketTimeout = SOCKET_TIMEOUT;
	}

}
