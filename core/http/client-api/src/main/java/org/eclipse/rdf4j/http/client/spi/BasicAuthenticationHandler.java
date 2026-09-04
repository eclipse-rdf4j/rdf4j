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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * {@link AuthenticationHandler} that adds HTTP Basic authentication credentials to every request.
 *
 * <p>
 * The {@code Authorization: Basic &lt;base64&gt;} header is computed once at construction time and appended to every
 * request passed to {@link #authenticate(HttpRequest)}.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * SPARQLProtocolSession session = ...;
 * session.setAuthenticationHandler(new BasicAuthenticationHandler("user", "secret"));
 * </pre>
 *
 * @see AuthenticationHandler
 */
public class BasicAuthenticationHandler implements AuthenticationHandler {

	private final String authorizationHeaderValue;

	/**
	 * Creates a new {@link BasicAuthenticationHandler} for the given credentials.
	 *
	 * @param username the username; must not be {@code null}
	 * @param password the password; must not be {@code null}
	 */
	public BasicAuthenticationHandler(String username, String password) {
		String encoded = Base64.getEncoder()
				.encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
		this.authorizationHeaderValue = "Basic " + encoded;
	}

	@Override
	public void authenticate(HttpRequest request) {
		request.addHeader("Authorization", authorizationHeaderValue);
	}
}
