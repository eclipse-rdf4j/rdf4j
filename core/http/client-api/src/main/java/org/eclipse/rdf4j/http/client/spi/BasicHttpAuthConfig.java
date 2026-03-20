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

/**
 * HTTP Basic authentication configuration.
 */
public final class BasicHttpAuthConfig implements HttpAuthConfig {

	private final String username;
	private final String password;
	private final String host;
	private final int port;

	private BasicHttpAuthConfig(String username, String password, String host, int port) {
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
	}

	/**
	 * Creates a Basic auth config applicable to any host/port.
	 *
	 * @param username the username.
	 * @param password the password.
	 * @return a new {@link BasicHttpAuthConfig}.
	 */
	public static BasicHttpAuthConfig of(String username, String password) {
		return new BasicHttpAuthConfig(username, password, null, -1);
	}

	/**
	 * Creates a Basic auth config scoped to a specific host and port.
	 *
	 * @param username the username.
	 * @param password the password.
	 * @param host     the target host (may be {@code null} for any host).
	 * @param port     the target port ({@code -1} for any port).
	 * @return a new {@link BasicHttpAuthConfig}.
	 */
	public static BasicHttpAuthConfig of(String username, String password, String host, int port) {
		return new BasicHttpAuthConfig(username, password, host, port);
	}

	/**
	 * @return the username for Basic authentication.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return the password for Basic authentication.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return the target host, or {@code null} if this auth applies to any host.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return the target port, or {@code -1} if this auth applies to any port.
	 */
	public int getPort() {
		return port;
	}
}
