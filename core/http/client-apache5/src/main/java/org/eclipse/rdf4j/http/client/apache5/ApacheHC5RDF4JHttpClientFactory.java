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
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClientConfig;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link RDF4JHttpClientFactory} implementation that uses Apache HttpComponents 5.
 *
 * <p>
 * Subclasses may override {@link #buildHttpClient(HttpClientBuilder, RDF4JHttpClientConfig)} to apply additional
 * configuration to the {@link HttpClientBuilder} before the client is constructed.
 */
public class ApacheHC5RDF4JHttpClientFactory implements RDF4JHttpClientFactory {

	@Override
	public String getName() {
		return "apache5";
	}

	@Override
	public RDF4JHttpClient create() {
		return create(RDF4JHttpClientConfig.defaultConfig());
	}

	/**
	 * Creates a new {@link RDF4JHttpClient} backed by an Apache HttpComponents 5 client configured from the given
	 * {@link RDF4JHttpClientConfig}.
	 *
	 * <p>
	 * The {@link HttpClientBuilder} is fully configured from {@code config} before being passed to
	 * {@link #buildHttpClient(HttpClientBuilder, RDF4JHttpClientConfig)}. Subclasses may override that method to apply
	 * additional customization before the client is built.
	 *
	 * @param config the client configuration; must not be {@code null}
	 * @return a new {@link RDF4JHttpClient}
	 */
	@Override
	public RDF4JHttpClient create(RDF4JHttpClientConfig config) {
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

		if (config.getSslContext().isPresent() || config.isDisableHostnameVerification()) {
			SSLContext sslContext = config.getSslContext().orElseGet(() -> {
				try {
					return SSLContext.getDefault();
				} catch (java.security.NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}
			});
			if (config.isDisableHostnameVerification()) {
				cmBuilder.setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE));
			} else {
				cmBuilder.setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext));
			}
		}

		PoolingHttpClientConnectionManager connectionManager = cmBuilder.build();

		// Request config
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getConnectionRequestTimeoutMs()))
				.setRedirectsEnabled(config.isFollowRedirects())
				.setCookieSpec(StandardCookieSpec.RELAXED)
				.build();

		HttpClientBuilder builder = HttpClients.custom()
				.evictExpiredConnections()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(requestConfig)
				.useSystemProperties()
				.setRetryStrategy(new RDF4JRetryStrategy(config.getMaxConnectionsPerRoute()));

		if (config.getIdleConnectionTimeoutMs() > 0) {
			builder.evictIdleConnections(TimeValue.ofMilliseconds(config.getIdleConnectionTimeoutMs()));
		}

		builder.setRedirectStrategy(DefaultRedirectStrategy.INSTANCE);

		if (!config.getDefaultHeaders().isEmpty()) {
			List<BasicHeader> defaultHeaders = config.getDefaultHeaders()
					.stream()
					.map(h -> new BasicHeader(h.getName(), h.getValue()))
					.collect(Collectors.toList());
			builder.setDefaultHeaders(defaultHeaders);
		}

		CloseableHttpClient httpClient = buildHttpClient(builder, config);
		return new ApacheHC5RDF4JHttpClient(httpClient, config.getMaxConnectionsPerRoute());
	}

	/**
	 * Builds the {@link CloseableHttpClient} from the supplied {@link HttpClientBuilder}.
	 *
	 * <p>
	 * The builder has already been fully configured by {@link #create(RDF4JHttpClientConfig)} when this method is
	 * called. Subclasses may override this method to apply additional configuration to the builder before calling
	 * {@code super.buildHttpClient(builder, config)}, or to replace the build entirely.
	 *
	 * @param builder the pre-configured builder; must not be {@code null}
	 * @param config  the client configuration that was used to configure the builder; must not be {@code null}
	 * @return the built {@link CloseableHttpClient}
	 */
	protected CloseableHttpClient buildHttpClient(HttpClientBuilder builder, RDF4JHttpClientConfig config) {
		return builder.build();
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
