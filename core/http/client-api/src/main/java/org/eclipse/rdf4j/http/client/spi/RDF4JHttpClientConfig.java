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
package org.eclipse.rdf4j.http.client.spi;

import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

/**
 * Immutable configuration for an {@link RDF4JHttpClient}.
 *
 * <p>
 * Instances are created via the {@link Builder}, which can be obtained from {@link #newBuilder()}. A default
 * configuration is available via {@link #defaultConfig()}.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * HttpClientConfig config = HttpClientConfig.newBuilder()
 * 		.connectTimeoutMs(5_000)
 * 		.socketTimeoutMs(30_000)
 * 		.followRedirects(true)
 * 		.build();
 * </pre>
 *
 * @see RDF4JHttpClient
 * @see RDF4JHttpClientFactory
 */
public final class RDF4JHttpClientConfig {

	private final int connectTimeoutMs;
	private final int socketTimeoutMs;
	private final int connectionRequestTimeoutMs;
	private final int maxConnectionsPerRoute;
	private final int maxConnectionsTotal;
	private final int maxRedirects;
	private final boolean followRedirects;
	private final long idleConnectionTimeoutMs;
	private final SSLContext sslContext;
	private final boolean disableHostnameVerification;
	private final List<HttpHeader> defaultHeaders;

	private RDF4JHttpClientConfig(Builder builder) {
		this.connectTimeoutMs = builder.connectTimeoutMs;
		this.socketTimeoutMs = builder.socketTimeoutMs;
		this.connectionRequestTimeoutMs = builder.connectionRequestTimeoutMs;
		this.maxConnectionsPerRoute = builder.maxConnectionsPerRoute;
		this.maxConnectionsTotal = builder.maxConnectionsTotal;
		this.maxRedirects = builder.maxRedirects;
		this.followRedirects = builder.followRedirects;
		this.idleConnectionTimeoutMs = builder.idleConnectionTimeoutMs;
		this.sslContext = builder.sslContext;
		this.disableHostnameVerification = builder.disableHostnameVerification;
		this.defaultHeaders = List.copyOf(builder.defaultHeaders);
	}

	/**
	 * Returns the connection timeout in milliseconds. This is the maximum time to wait for a connection to be
	 * established.
	 *
	 * @return connection timeout in milliseconds
	 */
	public int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	/**
	 * Returns the socket (read) timeout in milliseconds. This is the maximum time to wait for data after the connection
	 * is established. A value of {@code 0} means an infinite timeout.
	 *
	 * @return socket timeout in milliseconds, or {@code 0} for no timeout
	 */
	public int getSocketTimeoutMs() {
		return socketTimeoutMs;
	}

	/**
	 * Returns the connection request timeout in milliseconds. This is the maximum time to wait when requesting a
	 * connection from the connection pool.
	 *
	 * @return connection request timeout in milliseconds
	 */
	public int getConnectionRequestTimeoutMs() {
		return connectionRequestTimeoutMs;
	}

	/**
	 * Returns the maximum number of concurrent connections per route (i.e. per target host).
	 *
	 * @return maximum connections per route
	 */
	public int getMaxConnectionsPerRoute() {
		return maxConnectionsPerRoute;
	}

	/**
	 * Returns the maximum total number of concurrent connections across all routes.
	 *
	 * @return maximum total connections
	 */
	public int getMaxConnectionsTotal() {
		return maxConnectionsTotal;
	}

	/**
	 * Returns the maximum number of redirects to follow when {@link #isFollowRedirects()} is {@code true}.
	 *
	 * @return maximum number of redirects
	 */
	public int getMaxRedirects() {
		return maxRedirects;
	}

	/**
	 * Returns whether HTTP redirects are followed automatically.
	 *
	 * @return {@code true} if redirects are followed, {@code false} otherwise
	 */
	public boolean isFollowRedirects() {
		return followRedirects;
	}

	/**
	 * Returns the maximum time in milliseconds that a pooled connection may remain idle before being evicted. A value
	 * of {@code 0} disables idle eviction.
	 *
	 * @return idle connection timeout in milliseconds
	 */
	public long getIdleConnectionTimeoutMs() {
		return idleConnectionTimeoutMs;
	}

	/**
	 * Returns the {@link SSLContext} to use for HTTPS connections, if one has been configured.
	 *
	 * @return an {@link Optional} containing the configured {@link SSLContext}, or empty if none was set
	 */
	public Optional<SSLContext> getSslContext() {
		return Optional.ofNullable(sslContext);
	}

	/**
	 * Returns whether TLS hostname verification is disabled. When {@code true}, the client will accept certificates
	 * even if the hostname does not match the certificate's CN/SAN fields.
	 *
	 * <p>
	 * <strong>Warning:</strong> disabling hostname verification removes an important security check. Only use this in
	 * controlled environments (e.g. testing against self-signed certificates).
	 *
	 * @return {@code true} if hostname verification is disabled, {@code false} otherwise
	 */
	public boolean isDisableHostnameVerification() {
		return disableHostnameVerification;
	}

	/**
	 * Returns the default HTTP headers to be sent with every request.
	 *
	 * @return an unmodifiable list of default headers; never {@code null}, may be empty
	 */
	public List<HttpHeader> getDefaultHeaders() {
		return defaultHeaders;
	}

	/**
	 * Returns a new {@link RDF4JHttpClientConfig} with all settings at their default values.
	 *
	 * @return a default {@link RDF4JHttpClientConfig}
	 */
	public static RDF4JHttpClientConfig defaultConfig() {
		return newBuilder().build();
	}

	/**
	 * Returns a new {@link Builder} for constructing an {@link RDF4JHttpClientConfig}.
	 *
	 * @return a new {@link Builder}
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Builder for {@link RDF4JHttpClientConfig}.
	 *
	 * <p>
	 * Default values:
	 * <ul>
	 * <li>{@code connectTimeoutMs} = 30,000 ms</li>
	 * <li>{@code socketTimeoutMs} = 0 (infinite)</li>
	 * <li>{@code connectionRequestTimeoutMs} = 3,600,000 ms (1 hour)</li>
	 * <li>{@code maxConnectionsPerRoute} = 25</li>
	 * <li>{@code maxConnectionsTotal} = 50</li>
	 * <li>{@code maxRedirects} = 5</li>
	 * <li>{@code followRedirects} = {@code true}</li>
	 * <li>{@code idleConnectionTimeoutMs} = 300,000 ms (5 minutes)</li>
	 * <li>{@code sslContext} = none (uses JVM default)</li>
	 * <li>{@code disableHostnameVerification} = {@code false}</li>
	 * </ul>
	 */
	public static final class Builder {
		private int connectTimeoutMs = 30_000;
		private int socketTimeoutMs = 0;
		private int connectionRequestTimeoutMs = 3_600_000;
		private int maxConnectionsPerRoute = 25;
		private int maxConnectionsTotal = 50;
		private int maxRedirects = 5;
		private boolean followRedirects = true;
		private long idleConnectionTimeoutMs = 300_000L;
		private SSLContext sslContext;
		private boolean disableHostnameVerification = false;
		private List<HttpHeader> defaultHeaders = List.of();

		private Builder() {
		}

		/**
		 * Sets the connection timeout in milliseconds.
		 *
		 * @param connectTimeoutMs connection timeout in milliseconds; must be non-negative
		 * @return this builder
		 */
		public Builder connectTimeoutMs(int connectTimeoutMs) {
			this.connectTimeoutMs = connectTimeoutMs;
			return this;
		}

		/**
		 * Sets the socket (read) timeout in milliseconds. Use {@code 0} for an infinite timeout.
		 *
		 * @param socketTimeoutMs socket timeout in milliseconds; must be non-negative
		 * @return this builder
		 */
		public Builder socketTimeoutMs(int socketTimeoutMs) {
			this.socketTimeoutMs = socketTimeoutMs;
			return this;
		}

		/**
		 * Sets the connection request timeout in milliseconds. This is the maximum time to wait when requesting a
		 * connection from the connection pool.
		 *
		 * @param connectionRequestTimeoutMs connection request timeout in milliseconds; must be non-negative
		 * @return this builder
		 */
		public Builder connectionRequestTimeoutMs(int connectionRequestTimeoutMs) {
			this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
			return this;
		}

		/**
		 * Sets the maximum number of concurrent connections per route (i.e. per target host).
		 *
		 * @param maxConnectionsPerRoute maximum connections per route; must be positive
		 * @return this builder
		 */
		public Builder maxConnectionsPerRoute(int maxConnectionsPerRoute) {
			this.maxConnectionsPerRoute = maxConnectionsPerRoute;
			return this;
		}

		/**
		 * Sets the maximum total number of concurrent connections across all routes.
		 *
		 * @param maxConnectionsTotal maximum total connections; must be positive
		 * @return this builder
		 */
		public Builder maxConnectionsTotal(int maxConnectionsTotal) {
			this.maxConnectionsTotal = maxConnectionsTotal;
			return this;
		}

		/**
		 * Sets the maximum number of redirects to follow when redirect following is enabled.
		 *
		 * @param maxRedirects maximum number of redirects; must be non-negative
		 * @return this builder
		 */
		public Builder maxRedirects(int maxRedirects) {
			this.maxRedirects = maxRedirects;
			return this;
		}

		/**
		 * Sets whether HTTP redirects should be followed automatically.
		 *
		 * @param followRedirects {@code true} to follow redirects, {@code false} to disable
		 * @return this builder
		 */
		public Builder followRedirects(boolean followRedirects) {
			this.followRedirects = followRedirects;
			return this;
		}

		/**
		 * Sets the maximum time that a pooled connection may remain idle before being evicted. Use {@code 0} to disable
		 * idle eviction.
		 *
		 * @param idleConnectionTimeoutMs idle timeout in milliseconds; must be non-negative
		 * @return this builder
		 */
		public Builder idleConnectionTimeoutMs(long idleConnectionTimeoutMs) {
			this.idleConnectionTimeoutMs = idleConnectionTimeoutMs;
			return this;
		}

		/**
		 * Sets a custom {@link SSLContext} for HTTPS connections. If not set, the JVM default SSL context is used.
		 *
		 * @param sslContext the {@link SSLContext} to use, or {@code null} to use the JVM default
		 * @return this builder
		 */
		public Builder sslContext(SSLContext sslContext) {
			this.sslContext = sslContext;
			return this;
		}

		/**
		 * Disables TLS hostname verification. When set to {@code true}, the client will accept certificates even if the
		 * hostname does not match the certificate's CN/SAN fields.
		 *
		 * <p>
		 * <strong>Warning:</strong> disabling hostname verification removes an important security check. Only use this
		 * in controlled environments (e.g. testing against self-signed certificates with hostname mismatches).
		 *
		 * @param disableHostnameVerification {@code true} to disable hostname verification
		 * @return this builder
		 */
		public Builder disableHostnameVerification(boolean disableHostnameVerification) {
			this.disableHostnameVerification = disableHostnameVerification;
			return this;
		}

		/**
		 * Sets the default HTTP headers to be sent with every request.
		 *
		 * @param defaultHeaders the headers to send by default; must not be {@code null}
		 * @return this builder
		 */
		public Builder defaultHeaders(List<HttpHeader> defaultHeaders) {
			this.defaultHeaders = List.copyOf(defaultHeaders);
			return this;
		}

		/**
		 * Adds a single default HTTP header to be sent with every request.
		 *
		 * @param name  the header name; must not be {@code null}
		 * @param value the header value; must not be {@code null}
		 * @return this builder
		 */
		public Builder defaultHeader(String name, String value) {
			List<HttpHeader> current = this.defaultHeaders;
			List<HttpHeader> updated = new java.util.ArrayList<>(current);
			updated.add(HttpHeader.of(name, value));
			this.defaultHeaders = List.copyOf(updated);
			return this;
		}

		/**
		 * Builds a new {@link RDF4JHttpClientConfig} from the current builder state.
		 *
		 * @return a new {@link RDF4JHttpClientConfig}
		 */
		public RDF4JHttpClientConfig build() {
			return new RDF4JHttpClientConfig(this);
		}
	}
}
