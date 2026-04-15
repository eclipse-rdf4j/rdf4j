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
 * The {@code Authorization: Bearer <token>} header is added to every request passed to
 * {@link #authenticate(HttpRequest)}. The token can be supplied either as a static string or as a
 * {@link Producer}{@code <String>} that is invoked on each request, allowing dynamic tokens such as short-lived OAuth
 * access tokens to be refreshed transparently.
 *
 * <p>
 * Example usage with a static token:
 *
 * <pre>
 * session.setAuthenticationHandler(new BearerTokenAuthenticationHandler("my-token"));
 * </pre>
 *
 * <p>
 * Example usage with a dynamic token producer:
 *
 * <pre>
 * session.setAuthenticationHandler(new BearerTokenAuthenticationHandler(tokenStore::currentToken));
 * </pre>
 *
 * @see AuthenticationHandler
 */
public class BearerTokenAuthenticationHandler implements AuthenticationHandler {

	private final Producer<String> tokenProducer;

	/**
	 * Creates a new {@link BearerTokenAuthenticationHandler} for the given static token.
	 *
	 * @param token the bearer token; must not be {@code null}
	 */
	public BearerTokenAuthenticationHandler(String token) {
		Objects.requireNonNull(token, "token must not be null");
		this.tokenProducer = () -> token;
	}

	/**
	 * Creates a new {@link BearerTokenAuthenticationHandler} that obtains the bearer token by invoking the given
	 * {@link Producer} on each request.
	 *
	 * <p>
	 * Use this constructor when the token may change over time (e.g. a short-lived OAuth access token that is refreshed
	 * by the producer). The producer is called once per request; any checked exception it throws is wrapped in an
	 * {@link IllegalStateException}.
	 *
	 * @param tokenProducer a producer that returns the current bearer token; must not be {@code null}
	 */
	public BearerTokenAuthenticationHandler(Producer<String> tokenProducer) {
		Objects.requireNonNull(tokenProducer, "tokenProducer must not be null");
		this.tokenProducer = tokenProducer;
	}

	@Override
	public void authenticate(HttpRequest request) {
		try {
			request.addHeader("Authorization", "Bearer " + tokenProducer.produce());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to produce bearer token", e);
		}
	}
}
