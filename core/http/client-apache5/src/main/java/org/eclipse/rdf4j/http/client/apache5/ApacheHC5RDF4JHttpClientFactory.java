/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.client.apache5;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.eclipse.rdf4j.http.client.spi.BasicHttpAuthConfig;
import org.eclipse.rdf4j.http.client.spi.HttpAuthConfig;
import org.eclipse.rdf4j.http.client.spi.HttpClientConfig;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link RDF4JHttpClientFactory} implementation that uses Apache HttpComponents 5.
 */
public class ApacheHC5RDF4JHttpClientFactory implements RDF4JHttpClientFactory {

	@Override
	public String getName() {
		return "apache5";
	}

	@Override
	public RDF4JHttpClient create() {
		return create(HttpClientConfig.defaultConfig());
	}

	@Override
	public RDF4JHttpClient create(HttpClientConfig config) {
		return create(config, null);
	}

	@Override
	public RDF4JHttpClient create(HttpClientConfig config, HttpAuthConfig authConfig) {
		// Build connection manager
		PoolingHttpClientConnectionManagerBuilder cmBuilder = PoolingHttpClientConnectionManagerBuilder.create()
				.setMaxConnPerRoute(config.getMaxConnectionsPerRoute())
				.setMaxConnTotal(config.getMaxConnectionsTotal())
				.setDefaultSocketConfig(SocketConfig.custom()
						.setSoTimeout(config.getSocketTimeoutMs() > 0
								? Timeout.ofMilliseconds(config.getSocketTimeoutMs())
								: Timeout.DISABLED)
						.build())
				.setDefaultConnectionConfig(ConnectionConfig.custom()
						.setConnectTimeout(Timeout.ofMilliseconds(config.getConnectTimeoutMs()))
						.build());

		if (config.getSslContext().isPresent()) {
			cmBuilder.setTlsSocketStrategy(new DefaultClientTlsStrategy(config.getSslContext().get()));
		}

		PoolingHttpClientConnectionManager connectionManager = cmBuilder.build();

		// Request config
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getConnectionRequestTimeoutMs()))
				.setRedirectsEnabled(config.isFollowRedirects())
				.build();

		HttpClientBuilder builder = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(requestConfig)
				.evictExpiredConnections()
				.evictIdleConnections(TimeValue.of(30, TimeUnit.MINUTES))
				.setRetryStrategy(new RDF4JRetryStrategy(config.getMaxConnectionsPerRoute()))
				.setRedirectStrategy(DefaultRedirectStrategy.INSTANCE);

		// Authentication
		if (authConfig instanceof BasicHttpAuthConfig basicAuth) {
			BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
					new AuthScope(basicAuth.getHost(), basicAuth.getPort()),
					new UsernamePasswordCredentials(basicAuth.getUsername(),
							basicAuth.getPassword().toCharArray()));
			builder.setDefaultCredentialsProvider(credsProvider);
		}

		CloseableHttpClient httpClient = builder.build();
		return new ApacheHC5RDF4JHttpClient(httpClient, config.getMaxConnectionsPerRoute());
	}

	/**
	 * Combines stale-connection retry (once, on IOException) and HTTP 408 retry (up to pool-size + 1 times).
	 * <p>
	 * Mirrors the retry behaviour of the former Apache HC4-based implementation:
	 * <ul>
	 * <li>{@code RetryHandlerStale}: retries once when the server has closed a pooled connection (detected via
	 * {@link ConnectionClosedException} or {@link NoHttpResponseException}).</li>
	 * <li>{@code ServiceUnavailableRetryHandler}: retries on HTTP 408 (Request Timeout) when keep-alive is active, up
	 * to {@code maxConnectionsPerRoute + 1} times to drain all stale pooled connections.</li>
	 * </ul>
	 */
	private static final class RDF4JRetryStrategy implements HttpRequestRetryStrategy {

		private final Logger logger = LoggerFactory.getLogger(RDF4JRetryStrategy.class);

		private final int maxConnectionsPerRoute;

		RDF4JRetryStrategy(int maxConnectionsPerRoute) {
			this.maxConnectionsPerRoute = maxConnectionsPerRoute;
		}

		@Override
		public boolean retryRequest(HttpRequest request, IOException exception, int execCount, HttpContext context) {
			// Retry once to recover from a stale / closed pooled connection
			if (execCount > 1) {
				return false;
			}
			if (exception instanceof ConnectionClosedException || exception instanceof NoHttpResponseException) {
				logger.warn("Retrying after stale connection ({})", exception.getMessage());
				return true;
			}
			return false;
		}

		@Override
		public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
			// Only retry on HTTP 408 (Request Timeout)
			if (response.getCode() != HttpURLConnection.HTTP_CLIENT_TIMEOUT) {
				return false;
			}
			// When keepAlive is disabled every connection is fresh; a 408 there is unexpected
			if (!"true".equalsIgnoreCase(System.getProperty("http.keepAlive", "true"))) {
				return false;
			}
			// Worst-case: the whole pool has idled out on the server – need pool-size + 1 retries
			if (execCount > maxConnectionsPerRoute + 1) {
				return false;
			}
			logger.info("Retrying request after HTTP 408 (attempt {})", execCount);
			return true;
		}

		@Override
		public TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context) {
			return TimeValue.ofSeconds(1);
		}
	}
}
