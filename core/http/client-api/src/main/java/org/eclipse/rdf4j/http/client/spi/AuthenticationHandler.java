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
 * Intercepts an HTTP request before it is sent, allowing authentication credentials to be injected.
 *
 * <p>
 * Implementations modify the outgoing {@link HttpRequest} in place. The most common use case is adding an
 * {@code Authorization} header via {@link HttpRequest#addHeader(String, String)}.
 *
 * <p>
 * A handler is registered on a session via
 * {@link org.eclipse.rdf4j.http.client.SPARQLProtocolSession#setAuthenticationHandler(AuthenticationHandler)}.
 *
 * @see BasicAuthenticationHandler
 */
public interface AuthenticationHandler {

	/**
	 * Applies authentication credentials to the given request by modifying it in place.
	 *
	 * @param request the outgoing HTTP request to authenticate; never {@code null}
	 */
	void authenticate(HttpRequest request);
}
