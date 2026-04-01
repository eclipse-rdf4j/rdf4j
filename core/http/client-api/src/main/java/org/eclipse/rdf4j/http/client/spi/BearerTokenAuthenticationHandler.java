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

import java.util.Objects;

/**
 * {@link AuthenticationHandler} that adds an HTTP Bearer token to every request.
 *
 * <p>
 * The {@code Authorization: Bearer <token>} header is set from the token supplied at construction time and appended to
 * every request passed to {@link #authenticate(HttpRequest)}.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * SPARQLProtocolSession session = ...;
 * session.setAuthenticationHandler(new BearerTokenAuthenticationHandler("my-token"));
 * </pre>
 *
 * @see AuthenticationHandler
 */
public class BearerTokenAuthenticationHandler implements AuthenticationHandler {

	private final String authorizationHeaderValue;

	/**
	 * Creates a new {@link BearerTokenAuthenticationHandler} for the given token.
	 *
	 * @param token the bearer token; must not be {@code null}
	 */
	public BearerTokenAuthenticationHandler(String token) {
		Objects.requireNonNull(token, "token must not be null");
		this.authorizationHeaderValue = "Bearer " + token;
	}

	@Override
	public void authenticate(HttpRequest request) {
		request.addHeader("Authorization", authorizationHeaderValue);
	}
}
